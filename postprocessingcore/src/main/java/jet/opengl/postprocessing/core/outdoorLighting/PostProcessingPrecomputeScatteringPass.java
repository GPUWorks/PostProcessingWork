package jet.opengl.postprocessing.core.outdoorLighting;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPassOutputTarget;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.ProgramProperties;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.LogUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/6/2.
 */

final class PostProcessingPrecomputeScatteringPass extends PostProcessingRenderPass{

    private Texture2D m_ptex2DOccludedNetDensityToAtmTop;
    private Texture2D m_ptex2DAmbientSkyLight;

    private Texture3D m_ptex3DSingleScatteringSRV;
    private Texture3D m_ptex3DHighOrderScatteringSRV;
    private Texture3D m_ptex3DMultipleScatteringSRV;

    // intermediate textures
    private Texture3D m_ptex3DSctrRadiance;
    private Texture3D m_ptex3DInsctrOrderp;

    // Helper texture.
    private Texture2D m_ptex2DSphereRandomSamplingSRV;

    private RenderTechnique m_PrecomputeNetDensityToAtmTopTech;
    private RenderTechnique m_PrecomputeSingleSctrTech;
    private RenderTechnique m_ComputeSctrRadianceTech;
    private RenderTechnique m_ComputeScatteringOrderTech;
    private RenderTechnique m_AddScatteringOrderTech;
    private RenderTechnique m_PrecomputeAmbientSkyLightTech;

    private SharedData m_sharedData;

    public PostProcessingPrecomputeScatteringPass(SharedData sharedData, Texture2D tex2DAmbientSkyLight) {
        super("PrecomputeScattering");

        m_sharedData = sharedData;
        m_ptex2DAmbientSkyLight = tex2DAmbientSkyLight;

        setOutputTarget(PostProcessingRenderPassOutputTarget.INTERNAL);

        // output0:  m_PrecomputeNetDensityToAtmTopTech
        // output1:  m_ptex3DSingleScatteringSRV
        // output2:  m_ptex3DHighOrderScatteringSRV
        // output3:  m_ptex3DMultipleScatteringSRV
        set(0, 4);
    }

    private static ByteBuffer loadBinary(String file) throws IOException{
        @SuppressWarnings("resource")
        FileChannel in = new FileInputStream(file).getChannel();
        ByteBuffer buf;
        buf = ByteBuffer.allocateDirect((int)in.size()).order(ByteOrder.nativeOrder());

        in.read(buf);
        in.close();
        buf.flip();
        return buf;
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(m_ptex3DSingleScatteringSRV != null && !m_sharedData.m_bRecomputeSctrCoeffs)
            return;

        initResources();

        boolean debug = false;
        if(debug){
//    		System.out.println("PrecomputedOpticalDepthTexture: " + compareData(folder + "PrecomputedOpticalDepthTexture\\") + "\n");
//        	System.out.println("SingleScatterings: " + compareData(folder + "SingleScatterings\\") + "\n");
//        	System.out.println("HighOrderScattering: " + compareData(folder + "HighOrderScattering\\") + "\n");
//        	System.out.println("MultipleScattering: " + compareData(folder + "MultipleScattering\\") + "\n");;

            String folder = "E:\\OutdoorResources\\";
            ByteBuffer pixels = null;
            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

            int width = 32;
            int height = 128;
            final int depth = 64 * 16;
            try {
                pixels = loadBinary(folder + "SingleScatterings\\OpenGL.data");
//                m_ptex3DSingleScatteringSRV.bind();
                gl.glBindTexture(m_ptex3DSingleScatteringSRV.getTarget(), m_ptex3DSingleScatteringSRV.getTexture());
                gl.glTexSubImage3D(GLenum.GL_TEXTURE_3D, 0, 0, 0, 0, width, height, depth, GLenum.GL_RGBA, GLenum.GL_FLOAT, pixels);

                pixels = loadBinary(folder + "HighOrderScattering\\OpenGL.data");
//                m_ptex3DHighOrderScatteringSRV.bind();
                gl.glBindTexture(m_ptex3DHighOrderScatteringSRV.getTarget(), m_ptex3DHighOrderScatteringSRV.getTexture());
                gl.glTexSubImage3D(GLenum.GL_TEXTURE_3D, 0, 0, 0, 0, width, height, depth, GLenum.GL_RGBA, GLenum.GL_FLOAT, pixels);

                pixels = loadBinary(folder + "MultipleScattering\\OpenGL.data");
//                m_ptex3DMultipleScatteringSRV.bind();
                gl.glBindTexture(m_ptex3DMultipleScatteringSRV.getTarget(), m_ptex3DMultipleScatteringSRV.getTexture());
                gl.glTexSubImage3D(GLenum.GL_TEXTURE_3D, 0, 0, 0, 0, width, height, depth, GLenum.GL_RGBA, GLenum.GL_FLOAT, pixels);
//                m_ptex3DMultipleScatteringSRV.unbind();
//                GLError.checkError();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

//        TextureGL textureGLs[] = {
//                m_ptex2DSphereRandomSamplingSRV,
//                m_ptex3DMultipleScatteringSRV,
//                m_ptex2DOccludedNetDensityToAtmTop,
//                m_ptex2DSphereRandomSamplingSRV,
//        };
//
//        int[] textureUnits ={
//                RenderTechnique.TEX2D_SPHERE_RANDOM,
//                RenderTechnique.TEX3D_PREVIOUS_RADIANCE,
//                RenderTechnique.TEX2D_OCCLUDED_NET_DENSITY,
//                RenderTechnique.TEX2D_SPHERE_RANDOM,
//        };
//
//        context.bindTextures(textureGLs, textureUnits, null);

        createPrecomputedOpticalDepthTexture(context);
        createPrecomputeSingleScattering(context);
        createPrecomputeMultipleScattering(context);
        createAmbientSkyLightTexture(context);

//        saveBinaryData();
//        saveTextData();
    }

    private void createPrecomputedOpticalDepthTexture(PostProcessingRenderContext context){
        drawQuad(context, m_PrecomputeNetDensityToAtmTopTech, m_ptex2DOccludedNetDensityToAtmTop);

        String debugName = "PrecomputedOpticalDepthTexture";
        LogUtil.i(LogUtil.LogType.DEFAULT, "----------------------------" + debugName + "-----------------------------------------");
        ProgramProperties properties = GLSLUtil.getProperties(m_PrecomputeNetDensityToAtmTopTech.getProgram());
        LogUtil.i(LogUtil.LogType.DEFAULT, properties.toString());
    }

    private void createAmbientSkyLightTexture(PostProcessingRenderContext context){
//        m_ptex2DSphereRandomSamplingSRV.bind(ScatteringRenderTechnique.TEX2D_SPHERE_RANDOM, 0);
//        m_ptex3DMultipleScatteringSRV.bind(ScatteringRenderTechnique.TEX3D_PREVIOUS_RADIANCE, 0);
//
//        m_PrecomputeAmbientSkyLightTech.setupUniforms(mediaParams);
////    	m_RenderTargets.setTexture2DRenderTargets(m_ptex2DAmbientSkyLight.getTexture(), 0);
//        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, m_ptex2DAmbientSkyLight.getTarget(), m_ptex2DAmbientSkyLight.getTexture(), 0);
//        drawQuad(m_PrecomputeAmbientSkyLightTech, m_ptex2DAmbientSkyLight.getWidth(), m_ptex2DAmbientSkyLight.getHeight());
//
//        m_PrecomputeAmbientSkyLightTech.setDebugName("PrecomputeAmbientSkyLight");
//        m_PrecomputeAmbientSkyLightTech.printPrograminfo();

        if(m_ptex2DAmbientSkyLight != null)
            drawQuad(context, m_PrecomputeAmbientSkyLightTech, m_ptex2DAmbientSkyLight);
    }

    private void createPrecomputeSingleScattering(PostProcessingRenderContext context){
        final int sm_iPrecomputedSctrUDim = m_sharedData.m_ScatteringInitAttribs.m_iPrecomputedSctrUDim;
        final int sm_iPrecomputedSctrVDim = m_sharedData.m_ScatteringInitAttribs.m_iPrecomputedSctrVDim;
        final int sm_iPrecomputedSctrWDim = m_sharedData.m_ScatteringInitAttribs.m_iPrecomputedSctrWDim;
        final int sm_iPrecomputedSctrQDim = m_sharedData.m_ScatteringInitAttribs.m_iPrecomputedSctrQDim;

        context.bindTexture(m_ptex2DOccludedNetDensityToAtmTop, RenderTechnique.TEX2D_OCCLUDED_NET_DENSITY, 0);
        for(int uiDepthSlice = 0; uiDepthSlice < m_ptex3DSingleScatteringSRV.getDepth(); ++uiDepthSlice){
            int uiW = uiDepthSlice % sm_iPrecomputedSctrWDim;
            int uiQ = uiDepthSlice / sm_iPrecomputedSctrWDim;
            float f2WQX = ((float)uiW + 0.5f) / (float)sm_iPrecomputedSctrWDim;
            assert(0 < f2WQX && f2WQX < 1);
            float f2WQY = ((float)uiQ + 0.5f) / (float)sm_iPrecomputedSctrQDim;
            assert(0 < f2WQY && f2WQY < 1);

//            GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, m_ptex3DSingleScatteringSRV.getTexture(), 0, uiDepthSlice);
//            drawQuad(m_PrecomputeSingleSctrTech, sm_iPrecomputedSctrUDim, sm_iPrecomputedSctrVDim);

            context.setViewport(0,0, sm_iPrecomputedSctrUDim, sm_iPrecomputedSctrVDim);
            context.setVAO(null);
            context.setProgram(m_PrecomputeSingleSctrTech);
            m_sharedData.setUniforms(m_PrecomputeSingleSctrTech);
            m_PrecomputeSingleSctrTech.setWQ(f2WQX, f2WQY);
            context.setBlendState(null);
            context.setDepthStencilState(null);
            context.setRasterizerState(null);
            context.setRenderTargetLayer(m_ptex3DSingleScatteringSRV, uiDepthSlice);

            context.drawFullscreenQuad();
            if(GLCheck.CHECK)
                GLCheck.checkError();
        }

        String debugName = "PrecomputeSingleScattering";
        LogUtil.i(LogUtil.LogType.DEFAULT, "----------------------------" + debugName + "-----------------------------------------");
        ProgramProperties properties = GLSLUtil.getProperties(m_PrecomputeSingleSctrTech.getProgram());
        LogUtil.i(LogUtil.LogType.DEFAULT, properties.toString());
    }

    private void drawQuad(PostProcessingRenderContext context, RenderTechnique shader, Texture2D target){
        context.setViewport(0,0, target.getWidth(), target.getHeight());
        context.setVAO(null);
        context.setProgram(shader);
        m_sharedData.setUniforms(shader);

        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(target);

        context.drawFullscreenQuad();
    }

    void saveTextData(){
        String folder = "E:\\OutdoorResources2\\";

        saveTextData(folder + "PrecomputedOpticalDepthTexture\\OpenGL.txt", m_ptex2DOccludedNetDensityToAtmTop);
        saveTextData(folder + "SingleScatterings\\OpenGL.txt", m_ptex3DSingleScatteringSRV);
        saveTextData(folder + "HighOrderScattering\\OpenGL.txt", m_ptex3DHighOrderScatteringSRV);
        saveTextData(folder + "MultipleScattering\\OpenGL.txt", m_ptex3DMultipleScatteringSRV);
    }

    private static void saveTextData(String filename, TextureGL texture){
        try {
            DebugTools.saveTextureAsText(texture.getTarget(), texture.getTexture(), 0, filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void saveBinaryData(){
        boolean needSave = true;
        if(!needSave) return;
        String folder = "E:\\OutdoorResources2\\";

        saveBinaryData(folder + "PrecomputedOpticalDepthTexture\\OpenGL.data", m_ptex2DOccludedNetDensityToAtmTop);
        saveBinaryData(folder + "SingleScatterings\\OpenGL.data", m_ptex3DSingleScatteringSRV);
        saveBinaryData(folder + "HighOrderScattering\\OpenGL.data", m_ptex3DHighOrderScatteringSRV);
        saveBinaryData(folder + "MultipleScattering\\OpenGL.data", m_ptex3DMultipleScatteringSRV);
    }

    private static void saveBinaryData(String filename, TextureGL texture){
        ByteBuffer pixels = TextureUtils.getTextureData(texture.getTarget(), texture.getTexture(), 0, true);

        try {
            DebugTools.write(pixels, filename, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createPrecomputeMultipleScattering(PostProcessingRenderContext context){
        final int sm_iPrecomputedSctrUDim = m_sharedData.m_ScatteringInitAttribs.m_iPrecomputedSctrUDim;
        final int sm_iPrecomputedSctrVDim = m_sharedData.m_ScatteringInitAttribs.m_iPrecomputedSctrVDim;
        final int sm_iPrecomputedSctrWDim = m_sharedData.m_ScatteringInitAttribs.m_iPrecomputedSctrWDim;
        final int sm_iPrecomputedSctrQDim = m_sharedData.m_ScatteringInitAttribs.m_iPrecomputedSctrQDim;
        final int depth = sm_iPrecomputedSctrWDim * sm_iPrecomputedSctrQDim;
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        final int iNumScatteringOrders = 4;
        boolean bHighOrderRTVCleared = false;
        context.bindTexture(m_ptex2DSphereRandomSamplingSRV, RenderTechnique.TEX2D_SPHERE_RANDOM, 0);
        for(int iSctrOrder = 1; iSctrOrder < iNumScatteringOrders; ++iSctrOrder)
        {
            for(int iPass = 0; iPass < 3; ++iPass)
            {
                // Pass 0: compute differential in-scattering
                // Pass 1: integrate differential in-scattering
                // Pass 2: accumulate total multiple scattering
                Texture3D pSRV = null;
                int texureUnit = -1;
                RenderTechnique pRenderTech = null;
                Texture3D pRTVs = null;
                switch(iPass)
                {
                    case 0:
                        // Pre-compute the radiance of light scattered at a given point in given direction.
                        pRenderTech = m_ComputeSctrRadianceTech;
                        pRTVs = m_ptex3DSctrRadiance;
                        pSRV = (iSctrOrder == 1) ? m_ptex3DSingleScatteringSRV : m_ptex3DInsctrOrderp;
                        texureUnit = RenderTechnique.TEX3D_PREVIOUS_RADIANCE;
                        break;

                    case 1:
                        // Compute in-scattering order for a given point and direction
                        pRenderTech = m_ComputeScatteringOrderTech;
                        pRTVs = m_ptex3DInsctrOrderp;
                        pSRV = m_ptex3DSctrRadiance;
                        texureUnit = RenderTechnique.TEX3D_POINT_TWISE_RANDIANCE;
                        break;

                    case 2:
                        // Accumulate in-scattering
                        pRenderTech = m_AddScatteringOrderTech;
                        pRTVs = m_ptex3DHighOrderScatteringSRV;
                        pSRV = m_ptex3DInsctrOrderp;
                        texureUnit = RenderTechnique.TEX3D_PREVIOUS_RADIANCE;
                        break;
                }

                context.bindTexture(pSRV, texureUnit, m_sharedData.m_psamLinearClamp);

                for(int uiDepthSlice = 0; uiDepthSlice < depth; ++uiDepthSlice)
                {

                    // Set sun zenith and sun view angles
                    int uiW = uiDepthSlice % sm_iPrecomputedSctrWDim;
                    int uiQ = uiDepthSlice / sm_iPrecomputedSctrWDim;
                    float f2WQX = ((float)uiW + 0.5f) / (float)sm_iPrecomputedSctrWDim;
                    assert(0 < f2WQX && f2WQX < 1);
                    float f2WQY = ((float)uiQ + 0.5f) / (float)sm_iPrecomputedSctrQDim;
                    assert(0 < f2WQY && f2WQY < 1);

                    context.setViewport(0,0, sm_iPrecomputedSctrUDim, sm_iPrecomputedSctrVDim);
                    context.setVAO(null);
                    context.setProgram(pRenderTech);
                    m_sharedData.setUniforms(pRenderTech);
                    pRenderTech.setWQ(f2WQX, f2WQY);
                    pRenderTech.setDepthSlice(uiDepthSlice);
                    if(pRenderTech == m_AddScatteringOrderTech){
                        context.setBlendState(m_sharedData.m_additiveBlendBS);
                    }else{
                        context.setBlendState(null);
                    }
                    context.setDepthStencilState(null);
                    context.setRasterizerState(null);
                    context.setRenderTargetLayer(pRTVs, uiDepthSlice);
                    if(pRenderTech == m_AddScatteringOrderTech && !bHighOrderRTVCleared){
//                    	m_RenderTargets.clearRenderTarget(0, 0);
//                        GL30.glClearBufferfv(GL11.GL_COLOR, 0, new float[]{0,0,0,0});
                        FloatBuffer zeros = CacheBuffer.wrap(0,0,0,0f);
                        gl.glClearBufferfv(GLenum.GL_COLOR, 0, zeros);

                    }

                    context.drawFullscreenQuad();
                }

                if(pRenderTech == m_AddScatteringOrderTech){
                    bHighOrderRTVCleared = true;
//                    States.defaultBS();
                }
            }
        }

        // Combine single scattering and higher order scattering into single texture
        GLFuncProviderFactory.getGLFuncProvider().glCopyImageSubData(m_ptex3DSingleScatteringSRV.getTexture(), m_ptex3DSingleScatteringSRV.getTarget(), 0, 0, 0, 0,
                m_ptex3DMultipleScatteringSRV.getTexture(), m_ptex3DMultipleScatteringSRV.getTarget(), 0, 0, 0, 0,
                m_ptex3DSingleScatteringSRV.getWidth(), m_ptex3DSingleScatteringSRV.getHeight(), depth);


//        States.additiveBlendBS();
//        m_ptex3DHighOrderScatteringSRV.bind(ScatteringRenderTechnique.TEX3D_PREVIOUS_RADIANCE, 0); // TODO samplers
        context.bindTexture(m_ptex3DHighOrderScatteringSRV, RenderTechnique.TEX3D_PREVIOUS_RADIANCE, 0);
        for(int uiDepthSlice = 0; uiDepthSlice < depth; ++uiDepthSlice)
        {
//            GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, m_ptex3DMultipleScatteringSRV.getTexture(), 0, uiDepthSlice);
//            drawQuad( /*pContext, */
//                    m_AddScatteringOrderTech,
//                    sm_iPrecomputedSctrUDim, sm_iPrecomputedSctrVDim );

            context.setViewport(0,0, sm_iPrecomputedSctrUDim, sm_iPrecomputedSctrVDim);
            context.setVAO(null);
            context.setProgram(m_AddScatteringOrderTech);
            m_sharedData.setUniforms(m_AddScatteringOrderTech);
            m_AddScatteringOrderTech.setDepthSlice(uiDepthSlice);
            context.setBlendState(m_sharedData.m_additiveBlendBS);
            context.setDepthStencilState(null);
            context.setRasterizerState(null);
            context.setRenderTargetLayer(m_ptex3DMultipleScatteringSRV, uiDepthSlice);
            context.drawFullscreenQuad();
        }

//        context.setRenderTargetLayer(new Texture3D(), 0);

        {
            String debugName = "ComputeSctrRadiance";
            LogUtil.i(LogUtil.LogType.DEFAULT, "----------------------------" + debugName + "-----------------------------------------");
            ProgramProperties properties = GLSLUtil.getProperties(m_ComputeSctrRadianceTech.getProgram());
            LogUtil.i(LogUtil.LogType.DEFAULT, properties.toString());
        }

        {
            String debugName = "ComputeScatteringOrder";
            LogUtil.i(LogUtil.LogType.DEFAULT, "----------------------------" + debugName + "-----------------------------------------");
            ProgramProperties properties = GLSLUtil.getProperties(m_ComputeScatteringOrderTech.getProgram());
            LogUtil.i(LogUtil.LogType.DEFAULT, properties.toString());
        }
    }

    private void initResources(){
        if(m_ptex3DSingleScatteringSRV != null)
            return;

        final int sm_iPrecomputedSctrUDim = m_sharedData.m_ScatteringInitAttribs.m_iPrecomputedSctrUDim;
        final int sm_iPrecomputedSctrVDim = m_sharedData.m_ScatteringInitAttribs.m_iPrecomputedSctrVDim;
        final int sm_iPrecomputedSctrWDim = m_sharedData.m_ScatteringInitAttribs.m_iPrecomputedSctrWDim;
        final int sm_iPrecomputedSctrQDim = m_sharedData.m_ScatteringInitAttribs.m_iPrecomputedSctrQDim;

        final int sm_iNumPrecomputedHeights = 256;
        final int sm_iNumPrecomputedAngles = 256;

        Texture2DDesc netDensityToAtmTopTexDesc = new Texture2DDesc();
        {
            netDensityToAtmTopTexDesc.width = sm_iNumPrecomputedHeights;
            netDensityToAtmTopTexDesc.height = sm_iNumPrecomputedAngles;
            netDensityToAtmTopTexDesc.mipLevels = 1;
            netDensityToAtmTopTexDesc.arraySize = 1;
            netDensityToAtmTopTexDesc.format = GLenum.GL_RG32F;
            netDensityToAtmTopTexDesc.sampleCount = 1;
        }

        m_ptex2DOccludedNetDensityToAtmTop = TextureUtils.createTexture2D(netDensityToAtmTopTexDesc, null);

        Texture3DDesc precomputedSctrTexDesc = new Texture3DDesc
                (
                        sm_iPrecomputedSctrUDim, //UINT Width;
                        sm_iPrecomputedSctrVDim, //UINT Height;
                        sm_iPrecomputedSctrWDim * sm_iPrecomputedSctrQDim, //UINT ArraySize;
                        1,						 //UINT MipLevels;
                        GLenum.GL_RGB16F          // DXGI_FORMAT Format;
                );

        m_ptex3DSingleScatteringSRV = TextureUtils.createTexture3D(precomputedSctrTexDesc, null);
        m_ptex3DSingleScatteringSRV.setName("tex3DSingleScattering");
        m_ptex3DHighOrderScatteringSRV = TextureUtils.createTexture3D(precomputedSctrTexDesc, null);
        m_ptex3DHighOrderScatteringSRV.setName("tex3DHighOrderScattering");
        m_ptex3DMultipleScatteringSRV = TextureUtils.createTexture3D(precomputedSctrTexDesc, null);
        m_ptex3DMultipleScatteringSRV.setName("tex3DMultipleScattering");

        // We need higher precision to store intermediate data
        precomputedSctrTexDesc.format = GLenum.GL_RGB32F;
        m_ptex3DSctrRadiance = TextureUtils.createTexture3D(precomputedSctrTexDesc, null);
        m_ptex3DInsctrOrderp = TextureUtils.createTexture3D(precomputedSctrTexDesc, null);

//        Texture2DDesc ambientSkyLightTexDesc = new Texture2DDesc
//                (
//                        sm_iAmbientSkyLightTexDim,          //UINT Width;
//                        1,                                  //UINT Height;
//                        1,                                  //UINT MipLevels;
//                        1,                                  //UINT ArraySize;
//                        GL30.GL_RGBA16F,     				//DXGI_FORMAT Format;
//                        1,0                                 //DXGI_SAMPLE_DESC SampleDesc;
////		    	D3D11_USAGE_DEFAULT,                //D3D11_USAGE Usage;
////		    	D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET,           //UINT BindFlags;
////		    	0,                                  //UINT CPUAccessFlags;
////		        0,                                  //UINT MiscFlags;
//                );
//        m_ptex2DAmbientSkyLight = TextureUtils.createTexture2D(ambientSkyLightTexDesc, null);

        final int sm_iNumRandomSamplesOnSphere = 128;
        Texture2DDesc RandomSphereSamplingTexDesc = new Texture2DDesc
                (
                        sm_iNumRandomSamplesOnSphere,   //UINT Width;
                        1,                              //UINT Height;
                        1,                              //UINT MipLevels;
                        1,                              //UINT ArraySize;
                        GLenum.GL_RGB16F, 				//DXGI_FORMAT Format;
                        1                               //UINT SampleCount
                );

        FloatBuffer sphereSampling = CacheBuffer.getCachedFloatBuffer(3 * sm_iNumRandomSamplesOnSphere);
        for(int iSample = 0; iSample < sm_iNumRandomSamplesOnSphere; ++iSample)
        {
            float z = Numeric.random(-1, 1);
            float t = Numeric.random(0, 2.0f * Numeric.PI);
            double r = Math.sqrt( Math.max(1 - z*z, 0.f) );
            float x = (float) (r * Math.cos(t));
            float y = (float) (r * Math.sin(t));

            sphereSampling.put(x).put(y).put(z);
        }

        sphereSampling.flip();
//		ByteBuffer sphereSampling = null;
//		try {
//			sphereSampling = FileUtils.loadBinary("E:\\OutdoorResources\\SingleScatterings\\random.dat", true);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
        m_ptex2DSphereRandomSamplingSRV = TextureUtils.createTexture2D(RandomSphereSamplingTexDesc, new TextureDataDesc(GLenum.GL_RGB, GLenum.GL_FLOAT, sphereSampling));

        m_PrecomputeNetDensityToAtmTopTech = new RenderTechnique("PrecomputeNetDensityToAtmTop.frag");
        m_PrecomputeSingleSctrTech = new RenderTechnique("PrecomputeSingleScattering.frag");
        m_ComputeScatteringOrderTech = new RenderTechnique("ComputeScatteringOrder.frag");
        m_ComputeSctrRadianceTech  = new RenderTechnique("ComputeSctrRadiance.frag");
        m_AddScatteringOrderTech   = new RenderTechnique("AddScatteringOrder.frag");
        m_PrecomputeAmbientSkyLightTech = new RenderTechnique("PrecomputeAmbientSkyLight.frag");

        m_PassOutputs[0] = m_ptex2DOccludedNetDensityToAtmTop;
        m_PassOutputs[1] = m_ptex3DSingleScatteringSRV;
        m_PassOutputs[2] = m_ptex3DHighOrderScatteringSRV;
        m_PassOutputs[3] = m_ptex3DMultipleScatteringSRV;
    }

    @Override
    public void dispose() {
        releaseHelperResources();


        if(m_ptex2DOccludedNetDensityToAtmTop != null){
            m_ptex2DOccludedNetDensityToAtmTop.dispose();
            m_ptex2DOccludedNetDensityToAtmTop = null;
        }

        if(m_ptex3DSingleScatteringSRV != null){
            m_ptex3DSingleScatteringSRV.dispose();
            m_ptex3DSingleScatteringSRV = null;
        }

        if(m_ptex3DHighOrderScatteringSRV != null){
            m_ptex3DHighOrderScatteringSRV.dispose();
            m_ptex3DHighOrderScatteringSRV = null;
        }

        if(m_ptex3DMultipleScatteringSRV != null){
            m_ptex3DMultipleScatteringSRV.dispose();
            m_ptex3DMultipleScatteringSRV = null;
        }

        if(m_ptex2DAmbientSkyLight != null){
            m_ptex2DAmbientSkyLight.dispose();
            m_ptex2DAmbientSkyLight = null;
        }
    }

    public void releaseHelperResources(){
        if(m_ptex2DSphereRandomSamplingSRV != null){
            m_ptex2DSphereRandomSamplingSRV.dispose();
            m_ptex2DSphereRandomSamplingSRV = null;
        }

        if(m_ptex3DSctrRadiance != null){
            m_ptex3DSctrRadiance.dispose();
            m_ptex3DSctrRadiance = null;
        }

        if(m_ptex3DInsctrOrderp != null){
            m_ptex3DInsctrOrderp.dispose();
            m_ptex3DInsctrOrderp = null;
        }

        if(m_PrecomputeNetDensityToAtmTopTech != null){
            m_PrecomputeNetDensityToAtmTopTech.dispose();
            m_PrecomputeNetDensityToAtmTopTech = null;
        }

        if(m_PrecomputeSingleSctrTech != null){
            m_PrecomputeSingleSctrTech.dispose();
            m_PrecomputeSingleSctrTech = null;
        }

        if(m_ComputeSctrRadianceTech != null){
            m_ComputeSctrRadianceTech.dispose();
            m_ComputeSctrRadianceTech = null;
        }

        if(m_ComputeScatteringOrderTech != null){
            m_ComputeScatteringOrderTech.dispose();
            m_ComputeScatteringOrderTech = null;
        }

        if(m_AddScatteringOrderTech != null){
            m_AddScatteringOrderTech.dispose();
            m_AddScatteringOrderTech = null;
        }

        if(m_PrecomputeAmbientSkyLightTech != null){
            m_PrecomputeAmbientSkyLightTech.dispose();
            m_PrecomputeAmbientSkyLightTech = null;
        }
    }
}
