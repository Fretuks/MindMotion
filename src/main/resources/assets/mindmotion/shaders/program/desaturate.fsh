#version 150

uniform sampler2D DiffuseSampler;
uniform float Desat;

in vec2 texCoord;
in vec2 oneTexel; // required by sobel.vsh, even if unused

out vec4 fragColor;

void main() {
    vec4 col = texture(DiffuseSampler, texCoord);
    float gray = dot(col.rgb, vec3(0.2126, 0.7152, 0.0722));
    float d = clamp(Desat, 0.0, 1.0);
    fragColor = vec4(mix(col.rgb, vec3(gray), d), col.a);
}
