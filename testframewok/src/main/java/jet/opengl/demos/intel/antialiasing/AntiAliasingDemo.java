package jet.opengl.demos.intel.antialiasing;

import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.ui.NvTweakEnumi;
import com.nvidia.developer.opengl.ui.NvTweakVarBase;
import com.nvidia.developer.opengl.ui.NvUIEventResponse;
import com.nvidia.developer.opengl.ui.NvUIReaction;

import java.io.IOException;

import jet.opengl.demos.scene.SceneConfig;
import jet.opengl.demos.scenes.WireSphere;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessing;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.DebugTools;

/**
 * Created by mazhen'gui on 2017/10/17.
 */
public final class AntiAliasingDemo extends NvSampleApp {
    private static final int TECH_NONE = 0;
    private static final int TECH_CMAA = 1;
    private static final int TECH_SAA = 2;
    private static final int TECH_SMAA = 3;
    private static final int TECH_FXAA = 4;

    private static final int TECH_MSAA = 5;

    private WireSphere mScene;
    private CMAAEffect mCMAA;
    private SAAPostProcessGPU mSAA;
    private SMAAEffect mSMAA;
    private float mTotalTime;

    private int mAntiAliasingTechnique;
    private boolean mSaveScreenTex;
    private SceneConfig mConfig = new SceneConfig();
    private PostProcessing mFXAA;
    private PostProcessingFrameAttribs m_frameAttribs;

    @Override
    public void initUI() {
        mScene.onCreateUI(mTweakBar);
        mTweakBar.addButton("Save Screen Texture", 1);
        NvTweakVarBase varBase = mTweakBar.addEnum("AntiAliasing", createControl("mAntiAliasingTechnique"), new NvTweakEnumi[]{
                new NvTweakEnumi("None", TECH_NONE),
                new NvTweakEnumi("CMAA", TECH_CMAA),
                new NvTweakEnumi("SAA", TECH_SAA),
                new NvTweakEnumi("SMAA", TECH_SMAA),
                new NvTweakEnumi("FXAA", TECH_FXAA),
                new NvTweakEnumi("MSAA", TECH_MSAA),
        }, 2);

        // MSAA presets
        NvTweakEnumi samplePatterns[] =
        {
                new NvTweakEnumi( "MSAA 1x", 1 ),
                new NvTweakEnumi( "MSAA 2x", 2 ),
                new NvTweakEnumi( "MSAA 4x", 4 ),
                new NvTweakEnumi( "MSAA 8x", 8 ),
        };
        mTweakBar.subgroupSwitchStart(varBase);
        mTweakBar.subgroupSwitchCase(TECH_MSAA);
        mTweakBar.addEnum("MSAA Details", createControl("sampleCount", mConfig), samplePatterns,3);
        mTweakBar.subgroupSwitchEnd();
    }

    @Override
    protected void initRendering() {
        mScene = new WireSphere();
        mScene.setNVApp(this);
        mScene.initScene();

        mCMAA = new CMAAEffect();
        mCMAA.OnCreate();

        mSAA = new SAAPostProcessGPU();
        mSAA.OnCreate();

        mSMAA = new SMAAEffect();
        mSMAA.OnCreate(getGLContext().width(), getGLContext().height());
        GLCheck.checkError();

        mFXAA = new PostProcessing();
        m_frameAttribs = new PostProcessingFrameAttribs();

        getGLContext().setSwapInterval(0);
    }

    @Override
    public void display() {
        if(mAntiAliasingTechnique == TECH_MSAA) {
            mScene.setConfigs(mConfig);
        }else{
            int sampleCount = mConfig.sampleCount;
            mConfig.sampleCount = 1;
            mScene.setConfigs(mConfig);
            mConfig.sampleCount = sampleCount;
        }

        mScene.draw(true, true);

        if(mAntiAliasingTechnique == TECH_MSAA && mConfig.sampleCount > 1){
            mScene.resoveMultisampleTexture(GLenum.GL_COLOR_BUFFER_BIT);
            return;
        }

        if(mSaveScreenTex){
            mSaveScreenTex = false;

            if(mConfig.sampleCount == 1) {
                try {
                    DebugTools.saveTextureAsImageFile(mScene.getSceneColorTex(), "E:/textures/Antialiasing/ScreenShot.png");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if(mAntiAliasingTechnique == TECH_CMAA){
            // Ad-hoc CMAA-zoom setup using g_ZoomBox - we need to think of a nicer way to do this perhaps :)
            // The only problem is that the DbgDisplayZoomBoxPS from CMAA.hlsl displays detailed debug info on edges which needs
            // access to the algorithm buffers and constants so that's why it's not done using the g_ZoomBox
            Texture2D zoomBoxColourTex = null;
            Texture2D zoomBoxEdgesInfoTex = null;
//        if( CPUT_CHECKBOX_CHECKED == mpShowZoom->GetCheckboxState() )
//        {
//            zoomBoxColourTex    = g_CPUTZoomBox.GetZoomColourTxtNoAddRef();
//            zoomBoxEdgesInfoTex = g_CPUTZoomBox.GetZoomEdgeTxtNoAddRef();
//        }
            float EdgeThreshold = 1.0f / 13.0f;
            float NonDominantEdgeRemovalAmount = 0.35f;
            Texture2D m_SceneColor2SRV_UNORM = mScene.getSceneColorTex();
            mCMAA.Draw( /*mpContext,*/ mTotalTime, EdgeThreshold, NonDominantEdgeRemovalAmount,
//                m_SceneColor2->GetColorResourceView(), m_SceneColor2SRV_UNORM,
                    mScene.getSceneColorTex(), m_SceneColor2SRV_UNORM,
                    mScene.getSceneColorTex(), null,
//                (CPUT_CHECKBOX_CHECKED == mpShowEdge->GetCheckboxState())?(CMAAEffect::DT_ShowEdges):(CMAAEffect::DT_Normal),
                    CMAAEffect.DT_Normal,
                    zoomBoxColourTex, zoomBoxEdgesInfoTex );
        }else if(mAntiAliasingTechnique == TECH_SAA){

        }else if(mAntiAliasingTechnique == TECH_SMAA){

        }else if(mAntiAliasingTechnique == TECH_FXAA){
            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            m_frameAttribs.sceneColorTexture = mScene.getSceneColorTex();
            m_frameAttribs.outputTexture = mScene.getSceneColorTex();

            mFXAA.addFXAA(5);
            mFXAA.performancePostProcessing(m_frameAttribs);
//            LogUtil.i(LogUtil.LogType.DEFAULT, "FXAA");
        }

        mScene.resoveMultisampleTexture();

        GLCheck.checkError();
        mTotalTime += getFrameDeltaTime();
    }

    @Override
    protected int handleReaction(NvUIReaction react) {
        if(react.code  == 1){
            mSaveScreenTex = true;
            return NvUIEventResponse.nvuiEventHandled;
        }else
            return super.handleReaction(react);
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <=0)
            return;

        mScene.onResize(width, height);

        mCMAA.OnSize(width, height);
        mSAA.OnSize(width, height);
        mSMAA.OnSize(width, height);
        GLCheck.checkError();
    }

    @Override
    public void onDestroy() {
        mScene.dispose();
        mCMAA.dispose();
    }
}
