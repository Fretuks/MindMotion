#version 150

in vec4 Position;

uniform mat4 ProjMat;
uniform vec2 OutSize;
uniform float ShakeStrength;
uniform float Time;

out vec2 texCoord;

void main() {

    // screen-shake offset in *pixels*
    float px = sin(Time * 25.0 + Position.x * 5.0) * ShakeStrength * 20.0;
    float py = cos(Time * 20.0 + Position.y * 7.0) * ShakeStrength * 20.0;

    // convert from pixels → normalized coordinates
    vec2 offset = vec2(px / OutSize.x, py / OutSize.y);

    vec2 shaken = Position.xy + offset;

    texCoord = shaken / OutSize;

    vec4 outPos = ProjMat * vec4(shaken, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.0, 1.0);
}