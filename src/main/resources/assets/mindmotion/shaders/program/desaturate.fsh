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

vec2 applyShockwave(vec2 uv) {
    if (ShockwaveStrength <= 0.001) return uv;

    vec2 center = vec2(0.5);
    vec2 dir = uv - center;
    float dist = length(dir);

    // outwards pulse that quickly fades after the shockwave passes
    float wave = sin((dist - Time * 1.75) * 32.0) * 0.012 * ShockwaveStrength;
    float falloff = smoothstep(0.05, 0.35, dist) * (1.0 - smoothstep(0.35, 0.9, dist));
    wave *= falloff;

    if (dist > 0.0001) {
        dir = normalize(dir);
    }

    return uv + dir * wave;
}

vec4 sampleBlur(vec2 uv) {
    if (BlurAmount <= 0.001) return texture(DiffuseSampler, uv);

    float w = BlurAmount * 0.5;

    vec4 sum = vec4(0.0);
    sum += texture(DiffuseSampler, uv + oneTexel * vec2(-1, 0)) * w;
    sum += texture(DiffuseSampler, uv + oneTexel * vec2( 1, 0)) * w;
    sum += texture(DiffuseSampler, uv + oneTexel * vec2( 0,-1)) * w;
    sum += texture(DiffuseSampler, uv + oneTexel * vec2( 0, 1)) * w;

    vec4 center = texture(DiffuseSampler, uv) * (1.0 - w * 4.0);
    return center + sum;
}

void main() {
    vec2 shockUV = applyShockwave(texCoord);

    float intensity = clamp(
        PulseStrength * 0.6 + FlashStrength * 0.5 + Vignette * 0.35 + FogStrength * 0.2,
        0.0,
        1.0
    );
    vec2 center = vec2(0.5);
    vec2 fromCenter = shockUV - center;

    // Chromatic aberration that ramps up with insanity
    vec2 chromaOffset = fromCenter * (0.002 + intensity * 0.004);
    float r = texture(DiffuseSampler, shockUV + chromaOffset).r;
    float g = texture(DiffuseSampler, shockUV).g;
    float b = texture(DiffuseSampler, shockUV - chromaOffset).b;
    vec4 col = vec4(r, g, b, 1.0);

    // Blur
    vec4 blurred = sampleBlur(shockUV);
    col = mix(col, blurred, BlurAmount);

    // Grayscale
    float gray = dot(col.rgb, vec3(0.2126, 0.7152, 0.0722));
    col.rgb = mix(col.rgb, vec3(gray), clamp(Desat, 0.0, 1.0));

    // Fog
    vec3 fogColor = mix(vec3(0.75), vec3(0.6, 0.65, 0.8), intensity);
    col.rgb = mix(col.rgb, fogColor, FogStrength);

    // Stronger base vignette
    float dist = distance(texCoord, center);
    float vign = smoothstep(0.15, 0.6, dist);
    float vignetteBoost = mix(1.0, 1.6, intensity);
    col.rgb = mix(col.rgb, vec3(0.0), vign * Vignette * vignetteBoost);

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

    // Add subtle grain and contrast as insanity rises
    float grain = sin(dot(shockUV * vec2(120.0, 90.0), vec2(12.9898, 78.233)) + Time * 6.0);
    grain = (grain * 0.5 + 0.5) * intensity * 0.08;
    col.rgb += grain;

    float contrast = 1.0 + intensity * 0.35;
    col.rgb = (col.rgb - 0.5) * contrast + 0.5;
    col.rgb = clamp(col.rgb, 0.0, 1.0);
    fragColor = vec4(col.rgb, 1.0);
}
