layout(location = 0) out vec4 OutColor;

in VS_OUTPUT_HS_INPUT Input;
void main()
{
    float3 entry, exit;
    float  shadowTerm = 1.0f;
    float  segmentTransmittance = 1.0f;
//    [flatten]
    if (IntersectDynamicParticle(Input, entry, exit, segmentTransmittance))
    {
        float2 lightTexCoord = ProjectIntoLightTexCoord(entry);

        SurfaceData LitSurface = ConstructSurfaceData(entry, 0.0f.xxx);

        //return float4( LitSurface.positionView.xy, 1, 1 );
        //return float4( ProjectIntoAvsmLightTexCoord( LitSurface.positionView.xyz ).xy, 1, 1 );

        shadowTerm = ShadowContrib(LitSurface, Input);
    }

    float depthDiff = 1.0f;
    float3 ambient = float3(0.01f);
    float3 diffuse = float3(0.95f);

    float3 LightContrib = float3(0.9,0.9,1.0) * diffuse;

    // soft blend with solid geometry
    {
        float depthBufferViewspacePos = LoadScreenDepthViewspace( int2(Input.Position.xy) );

        depthDiff = (depthBufferViewspacePos - Input.ViewPos.z) / mSoftParticlesSaturationDepth;
        depthDiff = smoothstep(0.0f, 1.0f, depthDiff);
    }

    if(mUI.wireframe)
    {
        OutColor = Input.color;
    }
    else
    {
        float3 Color = ambient + LightContrib * shadowTerm;// * gParticleOpacityNoiseTex.Sample(gDiffuseSampler, Input.UVS.xy).xyz;
        OutColor = float4(Color, depthDiff * (1.0f - segmentTransmittance));
    }
}