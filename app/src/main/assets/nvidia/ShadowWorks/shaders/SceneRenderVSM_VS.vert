//----------------------------------------------------------------------------------
// File:        SoftShadows\assets\shaders/eyerender.vert
// SDK Version: v1.2 
// Email:       gameworks@nvidia.com
// Site:        http://developer.nvidia.com/
//
// Copyright (c) 2014, NVIDIA CORPORATION. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//  * Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//  * Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//  * Neither the name of NVIDIA CORPORATION nor the names of its
//    contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
// OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//----------------------------------------------------------------------------------
layout(location = 0) in vec3 g_vPosition;
layout(location = 1) in vec3 g_vNormal;

uniform mat4 g_world;
uniform mat4 g_viewProj;

uniform mat4 gLightView;
uniform mat4 gLightViewProj;
uniform vec3 gLightPos;

out float fDepth;
out vec4 lightViewPos;
out vec3 wLight;

out vec4 worldPosition;
out vec3 normal;


void main()
{
    vec4 worldPos = g_world * vec4(g_vPosition, 1.0);
    worldPosition = worldPos;
    normal = g_vNormal;
    gl_Position = g_viewProj * worldPos;

    lightViewPos = gLightViewProj * worldPos;
//    vec3 lightVec = gLightPos.xyz - worldPos.xyz;
    vec4 lightVec = gLightView * worldPos;
    fDepth = -(lightVec.z);
    wLight = lightVec.xyz / fDepth;

}
