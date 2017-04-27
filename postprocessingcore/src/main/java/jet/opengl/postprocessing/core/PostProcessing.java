package jet.opengl.postprocessing.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.core.radialblur.PostProcessingRadialBlurEffect;
import jet.opengl.postprocessing.core.radialblur.PostProcessingRadialBlurPass;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public class PostProcessing implements Disposeable{
    public static final String BLOOM = "BLOOM";
    public static final String FXAA = "FXAA";
    public static final String CLIP_TEXTURE = "CLIP_TEXTURE";
    public static final String GUASSION_BLUR = "GUASSION_BLUR";
    public static final String RADIAL_BLUR = "RADIAL_BLUR";
    public static final String FISH_EYE = "FISH_EYE";

    private static final int NUM_TAG_CACHE = 32;

    public static final int GUASSION_BLUR_PRIPORTY = 0;

    private PostProcessingRenderContext m_RenderContext;

    private final List<EffectTag> m_CurrentEffects = new ArrayList<>();
    private final List<EffectTag> m_PrevEffects    = new ArrayList<>();
    private final LinkedHashMap<String, PostProcessingRenderPass> m_AddedRenderPasses = new LinkedHashMap<>();
    private final PostProcessingParameters m_Parameters;
    private final Map<String, PostProcessingEffect> m_RegisteredEffects = new HashMap<>();
    private final EffectTag[] m_TagCaches = new EffectTag[NUM_TAG_CACHE];
    private int m_TagCount = 0;

    private PostProcessingRenderPass m_LastAddedPass;

    private boolean m_bEnablePostProcessing = true;
    private boolean m_SplitScreenDebug;
    private boolean m_bUsePortionTex;

    public PostProcessing(){
        m_Parameters = new PostProcessingParameters(this);
        registerEffect(new PostProcessingRadialBlurEffect());
    }

    public void registerEffect(PostProcessingEffect effect){
        if(GLCheck.CHECK){
            if(m_RegisteredEffects.containsKey(effect.getEffectName())){
                LogUtil.e(LogUtil.LogType.DEFAULT, "Dumplicate Effect: name = " + effect.getEffectName());
            }
        }

        m_RegisteredEffects.put(effect.getEffectName(), effect);
    }

    public void addEffect(String name, Object initParams, Object uniformParams){
        PostProcessingEffect effect = m_RegisteredEffects.get(name);
        if(effect == null)
            throw new NullPointerException("No found the Effect by name: " + name);
        effect.initValue = initParams;
        effect.uniformValue = uniformParams;
        m_CurrentEffects.add(obtain(name, effect.getPriority(), initParams, uniformParams));
    }

    /*
    Object getEffectInitValue(String name){
        EffectTag value = customeEffectData.get(name);
        if(value == null)
            return null;
        else
            return value.initValue;
    }

    Object getEffectUniformValue(String name){
        EffectTag value = customeEffectData.get(name);
        if(value == null)
            return null;
        else
            return value.uniformValue;
    }
*/
    public void performancePostProcessing(PostProcessingFrameAttribs frameAttribs){
        prepare(frameAttribs);

        GLStateTracker.getInstance().saveStates();
        try{
            if(!m_bEnablePostProcessing){
                if(frameAttribs.sceneColorTexture != null) {
                    m_RenderContext.renderTo(frameAttribs.sceneColorTexture, frameAttribs.outputTexture, frameAttribs.viewport);
                }
                return;
            }

            m_RenderContext.performancePostProcessing(frameAttribs.outputTexture);

//            GLStateTracker.getInstance().setVAO(null);
//            GLStateTracker.getInstance().setBlendState(null);
//            GLStateTracker.getInstance().setDepthStencilState(null);
//            GLStateTracker.getInstance().setRasterizerState(null);

            //      checkGLError();
            if (!m_AddedRenderPasses.isEmpty()) {
//                int size = m_AddedRenderPasses.size();
//                Texture2D src;
//                if(size > 2) {
//                    src = m_LastAddedPass.getOutputTexture(0);
//                }else{
//                    src = frameAttribs.sceneColorTexture;
//                }
//                if (m_bUsePortionTex) {
//                    // TODO The two step can combine in one pass.
//                    m_RenderContext.renderTo(frameAttribs.sceneColorTexture, frameAttribs.outputTexture, frameAttribs.viewport);
//                    m_RenderContext.renderTo(src, frameAttribs.outputTexture, frameAttribs.clipRect);
//                }
//                else {
//                    m_RenderContext.renderTo(src, frameAttribs.outputTexture, frameAttribs.viewport);
//                }
//                m_RenderContext.finish();
            }else /*if(m_OutputToScreen || frameAttribs.SceneColorBuffer != nullptr)*/{
                m_RenderContext.renderTo(frameAttribs.sceneColorTexture, frameAttribs.outputTexture, m_bUsePortionTex? frameAttribs.clipRect: frameAttribs.viewport);
            }

        }finally {
            GLStateTracker.getInstance().restoreStates();
            GLStateTracker.getInstance().reset();
        }
    }

    private EffectTag obtain(String name,int priority){
        return obtain(name, priority, null, null);
    }

    private EffectTag obtain(String name,int priority,Object initValue, Object uniformValue){
        if(m_TagCount > 0){
            m_TagCount--;
            EffectTag tag = m_TagCaches[m_TagCount];
            tag.name = name;
            tag.initValue = initValue;
            tag.uniformValue = uniformValue;
            tag.priority = priority;
            return tag;
        }else{
            return new EffectTag(name, priority, initValue, uniformValue);
        }
    }

    private void releaseTags(List<EffectTag> tags){
        final int offset=m_TagCount;
        final int end = Math.min(NUM_TAG_CACHE, m_TagCount + tags.size());

        for(; m_TagCount < end; m_TagCount ++){
            m_TagCaches[m_TagCount] = tags.get(m_TagCount - offset);
        }
        tags.clear();
    }

    private void initlizeContext(){
        if(m_RenderContext == null){
            m_RenderContext = new PostProcessingRenderContext();
            m_RenderContext.initlizeGL(0,0);
            m_RenderContext.m_Parameters = m_Parameters;
        }
    }

    private void prepare(PostProcessingFrameAttribs frameAttribs){
        initlizeContext();

        if(m_CurrentEffects.isEmpty() && m_PrevEffects.isEmpty()){
            return;
        }

        PostProcessingRenderPassInput colorInputPass = new PostProcessingRenderPassInput("SceneColor", frameAttribs.sceneColorTexture);
        PostProcessingRenderPassInput depthInputPass = new PostProcessingRenderPassInput("SceneDepth", frameAttribs.sceneDepthTexture);

        m_CurrentEffects.sort(null);
        if(m_CurrentEffects.size() != m_PrevEffects.size() || !m_CurrentEffects.equals(m_PrevEffects)){
            m_AddedRenderPasses.clear();
            m_LastAddedPass = null;

//            m_AddedRenderPasses.put("SceneColor", colorInputPass);  TODO maybe cause problems
//            m_AddedRenderPasses.put("SceneDepth", depthInputPass);

            for(EffectTag effectTag : m_CurrentEffects){
                PostProcessingEffect effect = m_RegisteredEffects.get(effectTag.name);
                effect.m_LastRenderPass = m_LastAddedPass;
                effect.fillRenderPass(this, colorInputPass, depthInputPass);
            }

            if(m_LastAddedPass != null) {
                m_LastAddedPass.setDependencies(0, 1);
            }

            m_RenderContext.setRenderPasses(m_AddedRenderPasses.values());
        }

        releaseTags(m_PrevEffects);
        m_PrevEffects.addAll(m_CurrentEffects);
        m_CurrentEffects.clear();
    }

    public void appendRenderPass(String name, PostProcessingRenderPass renderPass){
        if(renderPass == null){
            throw new NullPointerException("renderPass is null");
        }

        m_AddedRenderPasses.put(name, renderPass);
        m_LastAddedPass = renderPass;
    }

    /*
    public PostProcessingRenderPass getLastPass(){
        return m_LastAddedPass;
    }*/

    public PostProcessingRenderPass findPass(String name){
        return m_AddedRenderPasses.get(name);
    }

    public void addRadialBlur(float centerX, float centerY, int samples){
        m_Parameters.radialBlurCenterX =centerX;
        m_Parameters.radialBlurCenterY = centerY;
        m_Parameters.radialBlurSamples = samples;

        PostProcessingEffect effect = m_RegisteredEffects.get(RADIAL_BLUR);
        m_CurrentEffects.add(obtain(effect.getEffectName(), effect.getPriority(), null, null));
    }

    @Override
    public void dispose() {
        PostProcessingRadialBlurPass.releaseResources();
    }

    private static final class EffectTag implements Comparable<EffectTag>{
        String name;
        int priority;

        Object initValue;
        Object uniformValue;

        public EffectTag(String name, int priority, Object initValue, Object uniformValue) {
            this.name = name;
            this.priority = priority;
            this.initValue = initValue;
            this.uniformValue = uniformValue;
        }

        public EffectTag(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EffectTag effectTag = (EffectTag) o;

            if(priority != effectTag.priority)
                return false;

            if(!name.equals(effectTag.name))
                return false;

            return CommonUtil.equals(initValue, effectTag.initValue);

            // TODO igore the uniformValue, it dynamic
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + priority;
            result = 31 * result + (initValue != null ? initValue.hashCode(): 0);
            return result;
        }

        @Override
        public int compareTo(EffectTag effectTag) {
            return priority - effectTag.priority;
        }
    }
}
