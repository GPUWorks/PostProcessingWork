//----------------------------------------------------------------------------------
// File:        FaceWorks/samples/sample_d3d11/shaders/common.hlsli
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

#ifndef COMMON_HLSLI
#define COMMON_HLSLI

//#pragma pack_matrix(row_major)
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"
#include "resources.h"

#ifndef USE_UNIFORM_VAR
#define USE_UNIFORM_VAR 0
#endif

struct Vertex
{
//	float3		m_pos		/*: POSITION*/;
	float3		m_normal	/*: NORMAL*/;
	float2		m_uv		/*: UV*/;
	float3		m_tangent	/*: TANGENT*/;
	float		m_curvature /*: CURVATURE*/;
};

#if USE_UNIFORM_VAR
uniform float g_debug;
uniform float g_debugSlider0;		// Mapped to debug slider in UI
uniform float g_debugSlider1;		// ...
uniform float g_debugSlider2;		// ...
uniform float g_debugSlider3;		// ...

uniform float4x4	g_matWorldToClip;
uniform float3		g_posCamera;

uniform float3		g_vecDirectionalLight;
uniform float3		g_rgbDirectionalLight;

uniform float4x4	g_matWorldToUvzwShadow;
uniform float3x3	g_matWorldToUvzShadowNormal;	// Matrix for transforming normals to shadow map space

uniform float		g_vsmMinVariance;			// Minimum variance for variance shadow maps
uniform float		g_shadowSharpening;
uniform float		g_tessScale;				// Scale of adaptive tessellation

uniform float		g_deepScatterIntensity;		// Multiplier on whole deep scattering result
uniform float		g_deepScatterNormalOffset;	// Normal offset for shadow lookup to calculate thickness

uniform float		g_exposure;					// Exposure multiplier

#else
//cbuffer cbDebug : CB_DEBUG		// matches struct CbufDebug in util.h
layout(binding = CB_DEBUG) uniform cbDebug
{
	float		g_debug;			// Mapped to spacebar - 0 if up, 1 if down
	float		g_debugSlider0;		// Mapped to debug slider in UI
	float		g_debugSlider1;		// ...
	float		g_debugSlider2;		// ...
	float		g_debugSlider3;		// ...
};

//cbuffer cbFrame : CB_FRAME					// matches struct CbufFrame in util.h
layout(binding = CB_FRAME) uniform cbFrame
{
	float4x4	g_matWorldToClip;
	float3		g_posCamera;

	float3		g_vecDirectionalLight;
	float3		g_rgbDirectionalLight;

	float4x4	g_matWorldToUvzwShadow;
	float3x3	g_matWorldToUvzShadowNormal;	// Matrix for transforming normals to shadow map space
	float		dummy;						// Padding

	float		g_vsmMinVariance;			// Minimum variance for variance shadow maps
	float		g_shadowSharpening;
	float		g_tessScale;				// Scale of adaptive tessellation

	float		g_deepScatterIntensity;		// Multiplier on whole deep scattering result
	float		g_deepScatterNormalOffset;	// Normal offset for shadow lookup to calculate thickness

//    float		g_deepScatterFalloff;
	float		g_exposure;					// Exposure multiplier
};
#endif

#if 0
TextureCube<float3> g_texCubeDiffuse	: TEX_CUBE_DIFFUSE;
TextureCube<float3> g_texCubeSpec		: TEX_CUBE_SPEC;
Texture2D<float> g_texShadowMap			: TEX_SHADOW_MAP;
Texture2D<float2> g_texVSM				: TEX_VSM;
Texture2D g_texCurvatureLUT				: TEX_CURVATURE_LUT;
Texture2D g_texShadowLUT				: TEX_SHADOW_LUT;

SamplerState g_ssPointClamp				: SAMP_POINT_CLAMP;
SamplerState g_ssBilinearClamp			: SAMP_BILINEAR_CLAMP;
SamplerState g_ssTrilinearRepeat		: SAMP_TRILINEAR_REPEAT;
SamplerState g_ssTrilinearRepeatAniso	: SAMP_TRILINEAR_REPEAT_ANISO;
SamplerComparisonState g_scsPCF			: SAMP_PCF;
#else
layout(binding=TEX_CUBE_DIFFUSE)  uniform samplerCube g_texCubeDiffuse;
layout(binding=TEX_CUBE_SPEC)     uniform samplerCube g_texCubeSpec;
layout(binding=TEX_SHADOW_MAP)    uniform sampler2D g_texShadowMap;
layout(binding=TEX_VSM)           uniform sampler2D g_texVSM;
layout(binding=TEX_CURVATURE_LUT) uniform sampler2D g_texCurvatureLUT;
layout(binding=TEX_SHADOW_LUT)    uniform sampler2D g_texShadowLUT;
#endif

float square(float x) { return x*x; }

#endif // COMMON_HLSLI
