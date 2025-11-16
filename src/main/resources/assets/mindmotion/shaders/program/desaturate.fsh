#version 150

uniform sampler2D DiffuseSampler;
uniform float Desat;
uniform float BlurAmount;
uniform float Vignette;
uniform float FogStrength;
uniform float PulseStrength;
uniform float Time;
uniform float FlashStrength;
uniform float ShockwaveStrength;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

vec4 sampleBlur() {
    if (BlurAmount <= 0.001) return texture(DiffuseSampler, texCoord);

    float w = BlurAmount * 0.5;

    vec4 sum = vec4(0.0);
    sum += texture(DiffuseSampler, texCoord + oneTexel * vec2(-1, 0)) * w;
    sum += texture(DiffuseSampler, texCoord + oneTexel * vec2( 1, 0)) * w;
    sum += texture(DiffuseSampler, texCoord + oneTexel * vec2( 0,-1)) * w;
    sum += texture(DiffuseSampler, texCoord + oneTexel * vec2( 0, 1)) * w;

    vec4 center = texture(DiffuseSampler, texCoord) * (1.0 - w * 4.0);
    return center + sum;
}

void main() {
    vec4 col = texture(DiffuseSampler, texCoord);

    // Blur
    vec4 blurred = sampleBlur();
    col = mix(col, blurred, BlurAmount);

    // Grayscale
    float gray = dot(col.rgb, vec3(0.2126, 0.7152, 0.0722));
    col.rgb = mix(col.rgb, vec3(gray), clamp(Desat, 0.0, 1.0));

    // Fog
    col.rgb = mix(col.rgb, vec3(0.75), FogStrength);

    // Stronger base vignette
    float dist = distance(texCoord, vec2(0.5));
    float vign = smoothstep(0.15, 0.6, dist);
    col.rgb = mix(col.rgb, vec3(0.0), vign * Vignette);

    // Pulsating vignette toward black
    if (PulseStrength > 0.001) {
        float pulse = (sin(Time * 4.0) * 0.5 + 0.5);
        pulse *= PulseStrength;

        float pVig = smoothstep(0.2, 0.8, dist);
        col.rgb = mix(col.rgb, vec3(0.0), pVig * pulse);
    }
    if (FlashStrength > 0.001) {
        float wave = sin(Time * 12.0);  // slower: 12 instead of 40
        float intensity = (abs(wave) * 0.65) * FlashStrength; 
        // max 25% brightness shift even at 100% insanity
    
        // shift colors subtly instead of full white/black
        vec3 flashColor = wave > 0.0 
            ? vec3(0.9, 0.9, 1.0)   // pale bluish-white
            : vec3(0.05, 0.05, 0.07); // deep blue-black
    
        col.rgb = mix(col.rgb, flashColor, intensity);
    }
    fragColor = vec4(col.rgb, 1.0);
}