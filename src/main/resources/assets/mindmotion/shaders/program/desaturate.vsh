#version 150

in vec4 Position;

uniform mat4 ProjMat;
uniform vec2 OutSize;
uniform float ShakeStrength;
uniform float Time;

out vec2 oneTexel;
out vec2 texCoord;

void main() {
    oneTexel = 1.0 / OutSize;
    vec2 uv = Position.xy;
    vec2 posPx = Position.xy;
    if (Position.x > 2.0 || Position.y > 2.0) {
        uv = Position.xy / OutSize;
        posPx = Position.xy;
    } else {
        uv = Position.xy;
        posPx = Position.xy * OutSize;
    }
    float px = sin(Time * 25.0 + posPx.x * 0.01) * ShakeStrength * 20.0;
    float py = cos(Time * 20.0 + posPx.y * 0.01) * ShakeStrength * 20.0;
    vec2 uvOffset = vec2(px / OutSize.x, py / OutSize.y);
    texCoord = uv + uvOffset;
    vec4 outPos = ProjMat * vec4(posPx, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.0, 1.0);
}