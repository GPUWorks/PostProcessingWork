#include "GBuffer.glsl"
#include "AVSM_Gen.glsl"

//layout(early_fragment_tests) in;
in _DynamicParticlePSIn
{
//    float4 Position  : SV_POSITION;
    float3 UVS		 /*: TEXCOORD0*/;
    float  Opacity	 /*: TEXCOORD1*/;
    float3 ViewPos	 /*: TEXCOORD2*/;
    float3 ObjPos    /*: TEXCOORD3*/;
    float3 ViewCenter/*: TEXCOORD4*/;
    float4 color      /*: COLOR*/;
    float2 ShadowInfo /*: TEXCOORD5*/;
}Input;

void main()
{
    // Initialize Intel Shader Extensions
//    	IntelExt_Init();

        float3 entry, exit;
        float  segmentTransmittance;
        DynamicParticlePSIn _Input;
        _Input.UVS = Input.UVS;
        _Input.Opacity = Input.Opacity;
        _Input.ViewPos = Input.ViewPos;
        _Input.ObjPos = Input.ObjPos;
        _Input.ViewCenter = Input.ViewCenter;
        _Input.color = Input.color;
        _Input.ShadowInfo = Input.ShadowInfo;

    	IntersectDynamicParticle(_Input, entry, exit, segmentTransmittance); // does the ray tracing with sphere for transmittence amount + thickness (thickness doesn't get used yet)
    	if (segmentTransmittance > 0.0)
        {
    		// From now on serialize all UAV accesses (with respect to other fragments shaded in flight which map to the same pixel)
//    		IntelExt_BeginPixelShaderOrdering();

            const int2 pixelAddr = int2(gl_FragCoord.xy);

    #ifndef AVSM_GEN_SOFT
            const float shadowZBias = 0.9; // shift sample a bit away from shadow source to avoid self-shadowing due to shadow sample cube size (less required for bigger shadowmap dimensions and/or more nodes, and much less in case linear shadow function is added)
            entry.z += shadowZBias;
    		exit.z  += shadowZBias;
    #endif

            float ctrlSurface = AVSMGenLoadControlSurfaceUAV(pixelAddr);
            if (0.0 == ctrlSurface) {
                AVSMGenData avsmData;

                // Clear and initialize avsm data with just one fragment
    #ifdef AVSM_GEN_SOFT
                AVSMGenInitDataSoft(avsmData, entry.z, exit.z, segmentTransmittance);
    #else
                AVSMGenInitData(avsmData, entry.z, segmentTransmittance);
    #endif

    			// Store AVSM data
                AVSMGenStoreRawDataUAV(pixelAddr, avsmData);

    			// Update control surface
    			ctrlSurface = 1;
                AVSMGenStoreControlSurface(pixelAddr, ctrlSurface);
            } else {
                AVSMGenNode nodeArray[AVSM_NODE_COUNT];

                AVSMGenLoadDataUAV(pixelAddr, nodeArray);
    #ifdef AVSM_GEN_SOFT
                AVSMGenInsertFragmentSoft(entry.z, exit.z, segmentTransmittance, nodeArray);
    #else
                AVSMGenInsertFragment(entry.z, segmentTransmittance, nodeArray);
    #endif
                AVSMGenStoreDataUAV(pixelAddr, nodeArray);
            }
    	}
}