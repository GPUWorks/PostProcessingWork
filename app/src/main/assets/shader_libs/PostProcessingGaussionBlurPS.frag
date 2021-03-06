
#include "PostProcessingCommonPS.frag"

uniform vec2 g_HalfPixelSize;
uniform sampler2D g_Texture;
uniform float g_Weights[SAMPLES];
uniform float g_Offsets[SAMPLES];

// automatically generated by GenerateGaussFunctionCode in GaussianBlur.h                                                                                            
vec3 GaussianBlur( sampler2D tex0, vec2 centreUV, vec2 halfPixelOffset, vec2 pixelOffset )                                                                           
{
//    return texture( g_Texture, UVAndScreenPos.xy  ).xyz;
    vec3 colOut = vec3( 0, 0, 0 );                                                                                                                                   
                                                                                                                                                                     
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////;
    // Kernel width 27 x 27
    //

    /*
    const float gWeights[stepCount] =float[stepCount](
       0.14090,
       0.15927,
       0.10715,
       0.05747,
       0.02457,
       0.00837,
       0.00228
    );
    const float gOffsets[stepCount] =float[stepCount](
       0.66025,
       2.46415,
       4.43572,
       6.40771,
       8.38028,
       10.35359,
       12.32779
    );
    */
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////;
                                                                                                                                                                     
    for( int i = 0; i < SAMPLES; i++ )
    {                                                                                                                                                                
        vec2 texCoordOffset = g_Offsets[i] * pixelOffset;
        vec3 col = texture( g_Texture, centreUV + texCoordOffset ).xyz + texture( g_Texture, centreUV - texCoordOffset ).xyz;
        colOut += g_Weights[i] * col;
    }                                                                                                                                                                

    return colOut;                                                                                                                                                   
}                                                                                                                                                                    

	void main()
	{
		Out_f4Color.xyz = GaussianBlur(g_Texture, m_f4UVAndScreenPos.xy, vec2(/*uRTPixelSizePixelSizeHalf.z,*/ 0), g_HalfPixelSize);
		Out_f4Color.w = 1.0;
	}