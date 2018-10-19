//----------------------------------------------------------------------------------
// File:        FaceWorks/samples/sample_d3d11/shaders/world_vs.hlsl
// SDK Version: v1.0
// Email:       gameworks@nvidia.com
// Site:        http://developer.nvidia.com/
//
// Copyright (c) 2014-2016, NVIDIA CORPORATION. All rights reserved.
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

#include "common.glsl"

layout(location =0) in float3		In_pos		/*: POSITION*/;
layout(location =1) in float3		In_normal	/*: NORMAL*/;
layout(location =2) in float2		In_uv		/*: UV*/;
layout(location =3) in float3		In_tangent	/*: TANGENT*/;
layout(location =4) in float		In_curvature /*: CURVATURE*/;

out VertexThrough
{
//    float       m_pos;
    float3		m_normal	/*: NORMAL*/;
    float2		m_uv		/*: UV*/;
    float3		m_tangent	/*: TANGENT*/;
    float		m_curvature /*: CURVATURE*/;
}o_vtx;
out float3 o_vecCamera;
out float4 o_uvzwShadow;

out gl_PerVertex
{
    vec4 gl_Position;
};

void main(
	/*in Vertex i_vtx,
	out Vertex o_vtx,
	out float3 o_vecCamera : CAMERA,
	out float4 o_uvzwShadow : UVZW_SHADOW,
	out float4 o_posClip : SV_Position*/)
{
    o_vtx.m_normal = In_normal;
    o_vtx.m_uv = In_uv;
    o_vtx.m_tangent = In_tangent;
    o_vtx.m_curvature = In_curvature;
	o_vecCamera = g_posCamera - In_pos;
//	o_vtx.m_pos = In_pos;

	o_uvzwShadow = mul(float4(In_pos, 1.0), g_matWorldToUvzwShadow);
	gl_Position = mul(float4(In_pos, 1.0), g_matWorldToClip);
}
