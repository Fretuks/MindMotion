#version 150

uniform sampler2D DiffuseSampler;
uniform float Desat;
uniform float BlurAmount;
uniform float Vignette;
uniform float FogStrength;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

vec4 sampleBlur() {
    if (BlurAmount <= 0.001) return texture(DiffuseSampler, texCoord);

    float w = BlurAmount * 0.5;

    vec4 sum = vec4(0.0);
    sum += texture(DiffuseSampler, texCoord + oneTexel * vec2(-1,  0)) * w;
    sum += texture(DiffuseSampler, texCoord + oneTexel * vec2( 1,  0)) * w;
    sum += texture(DiffuseSampler, texCoord + oneTexel * vec2( 0, -1)) * w;
    sum += texture(DiffuseSampler, texCoord + oneTexel * vec2( 0,  1)) * w;

    vec4 center = texture(DiffuseSampler, texCoord) * (1.0 - w * 4.0);

    return center + sum;
}

void main() {
    vec4 col = texture(DiffuseSampler, texCoord);
    vec4 blurred = sampleBlur();
    col = mix(col, blurred, BlurAmount);

    float gray = dot(col.rgb, vec3(0.2126, 0.7152, 0.0722));
    col.rgb = mix(col.rgb, vec3(gray), clamp(Desat, 0.0, 1.0));

    col.rgb = mix(col.rgb, vec3(0.75, 0.75, 0.8), FogStrength);

    float dist = distance(texCoord, vec2(0.5, 0.5));
    float vign = smoothstep(0.3, 0.7, dist);
    col.rgb = mix(col.rgb, col.rgb * 0.4, vign * Vignette);

    fragColor = vec4(col.rgb, 1.0);
}