///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2016, Intel Corporation
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
// documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
// the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of 
// the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
// SOFTWARE.
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// File changes (yyyy-mm-dd)
// 2016-09-07: filip.strugar@intel.com: first commit (extracted from VertexAsylum codebase, 2006-2016 by Filip Strugar)
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#include "vaShared.glsl"

#include "vaSimpleShadowMap.glsl"

//cbuffer SimpleMeshPerInstanceConstantsBuffer : register( b1 )
layout(binding = 1) uniform SimpleMeshPerInstanceConstantsBuffer
{
    ShaderSimpleMeshPerInstanceConstants      g_PerInstanceConstants;
};

layout(binding = 1) uniform sampler2D           g_textureColor; //              : register(t0);
// Texture2D           g_textureNormal             : register(t1);
// Texture2D           g_textureHeight             : register(t2);

in  GenericSceneVertexTransformed
{
   float4 Position             ;//: SV_Position;
   float4 Color                ;//: COLOR;
   float4 ViewspacePos         ;//: TEXCOORD0;
   float4 ViewspaceNormal      ;//: NORMAL0;
   float4 ViewspaceTangent     ;//: NORMAL1;
   float4 ViewspaceBitangent   ;//: NORMAL2;
   float4 Texcoord0            ;//: TEXCOORD1;
}_input;

layout(location = 0) out float4 Out_Color;

float4 SimpleMeshColor( /*const GenericSceneVertexTransformed input,*/ float shadowTerm, const bool isFrontFace )
{
    const float3 diffuseLightVector     = -g_Global.Lighting.DirectionalLightViewspaceDirection.xyz;

    // material props
    float4 color                        = _input.Color; //g_textureColor.Sample( g_samplerAnisotropicWrap, input.Texcoord0.xy );
    float3 normal                       = float3( 0.0, 0.0, 1.0 ); //UnpackNormal( g_textureNormal.Sample( g_samplerAnisotropicWrap, input.Texcoord0.xy) );
    float3 viewDir                      = normalize( _input.ViewspacePos.xyz );

    float3x3 tangentSpace   = float3x3(
			                            normalize( _input.ViewspaceTangent.xyz   ),
			                            normalize( _input.ViewspaceBitangent.xyz ),
                                        normalize( _input.ViewspaceNormal.xyz    ) );

    normal                  = mul( normal, tangentSpace );

    if( !isFrontFace )
        normal = -normal;

    // start calculating final colour
    float3 lightAccum       = color.rgb * g_Global.Lighting.AmbientLightIntensity.rgb;

    // directional light
    float nDotL             = dot( normal, diffuseLightVector );

    float3 reflected        = diffuseLightVector - 2.0*nDotL*normal;
	float rDotV             = saturate( dot(reflected, viewDir) );
    float specular          = saturate( pow( saturate( rDotV ), 8.0 ) );

    // facing towards light: front and specular
    float lightFront        = saturate( nDotL ) * shadowTerm;
    lightAccum              += lightFront * color.rgb * g_Global.Lighting.DirectionalLightIntensity.rgb;
    //lightAccum += fakeSpecularMul * specular;

    float3 finalColor       = saturate( lightAccum );
    finalColor              = saturate( finalColor );

    // input.ViewspacePos.w is distance to camera
    finalColor              = FogForwardApply( finalColor, _input.ViewspacePos.w );

    return float4( finalColor, 1.0 );
}

#if 0
GenericSceneVertexTransformed SimpleMeshVS( const in GenericSceneVertex input )
{
    GenericSceneVertexTransformed ret;

    ret.Color                   = input.Color;
    ret.Texcoord0               = input.Texcoord;

    ret.ViewspacePos            = mul( g_PerInstanceConstants.WorldView, float4( input.Position.xyz, 1) );
    ret.ViewspaceNormal.xyz     = normalize( mul( g_PerInstanceConstants.WorldView, float4(input.Normal.xyz, 0.0) ).xyz );
    ret.ViewspaceTangent.xyz    = normalize( mul( g_PerInstanceConstants.WorldView, float4(input.Tangent.xyz, 0.0) ).xyz );
    ret.ViewspaceBitangent.xyz  = normalize( cross( ret.ViewspaceNormal.xyz, ret.ViewspaceTangent.xyz) );

    // distance to camera
    ret.ViewspacePos.w          = length( ret.ViewspacePos.xyz );

    ret.Position                = mul( g_Global.Proj, float4( ret.ViewspacePos.xyz, 1.0 ) );

    ret.ViewspaceNormal.w       = 0.0;
    ret.ViewspaceTangent.w      = 0.0;
    ret.ViewspaceBitangent.w    = 0.0;

    return ret;
}

float4 SimpleMeshGenerateShadowVS( const in GenericSceneVertex input ) : SV_Position
{
    return mul( g_PerInstanceConstants.ShadowWorldViewProj, float4( input.Position.xyz, 1) );
}
#endif

#if SimpleMeshPS

//float4 SimpleMeshPS( const in GenericSceneVertexTransformed input, const bool isFrontFace : SV_IsFrontFace ) : SV_Target
void main()
{
    bool isFrontFace = gl_IsFrontFace;
    if( g_Global.WireframePass > 0.0 )
    {
        Out_Color = float4( 0.5, 0.0, 0.0, 1.0 );
        return;
    }

    Out_Color = SimpleMeshColor( /*input,*/ 1.0, isFrontFace );
}

#elif SimpleMeshShadowedPS

//float4 SimpleMeshShadowedPS( const in GenericSceneVertexTransformed input, const bool isFrontFace : SV_IsFrontFace ) : SV_Target
void main()
{
    bool isFrontFace = gl_IsFrontFace;
    if( g_Global.WireframePass > 0.0 )
    {
        Out_Color = float4( 0.5, 0.0, 0.0, 1.0 );
        return;
    }

    float shadowTerm = SimpleShadowMapSample( _input.ViewspacePos.xyz );

    Out_Color =  SimpleMeshColor( shadowTerm, isFrontFace );
}

#endif

