package jet.opengl.demos.nvidia.waves.samples;

import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.util.Random;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.FullscreenProgram;
import jet.opengl.postprocessing.shader.VisualDepthTextureProgram;
import jet.opengl.postprocessing.texture.FramebufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/8/2.
 */

final class CTerrainOcean {
    static final float main_buffer_size_multiplier = 1.1f;
    static final float reflection_buffer_size_multiplier = 1.1f;
    static final float refraction_buffer_size_multiplier = 1.1f;
    static final float scene_z_near = 1.0f;
    static final float scene_z_far = 25000.0f;
    static final float camera_fov = 110.0f;

    static final int sky_gridpoints = 10;
    static final float sky_texture_angle = 0.425f;
    static final int water_normalmap_resource_buffer_size_xy = 2048;
    static final int shadowmap_resource_buffer_size_xy = 2048;

    Texture2D rock_bump_texture;
    Texture2D sky_texture;
    Texture2D foam_intensity_perlin2;
    Texture2D foam24bit;

    FramebufferGL reflection_framebuffer;
    FramebufferGL   refraction_framebuffer;
    FramebufferGL   shadownmap_framebuffer;
    FramebufferGL   water_normalmap_framebuffer;
    FramebufferGL   main_color_framebuffer;

    int backbufferWidth, backbufferHeight;
    Random random = new Random(123);

    float[] clearColor = {0.8f, 0.8f, 1.0f, 1.0f};
    float[] refractionClearColor = {0.5f, 0.5f, 0.5f, 1.0f};

    final Vector3f camera_position = new Vector3f();
    final Vector3f camera_direction = new Vector3f();
    final Matrix4f camera_modelView = new Matrix4f();
    final Matrix4f camera_mvp = new Matrix4f();
    final Matrix4f camera_projection = new Matrix4f();

    TextureSampler[] terrain_textures;

    IsRenderHeightfieldProgram renderHeightfieldProgram;
    IsWaterNormalmapCombineProgram waterNormalmapCombineProgram;
    IsMainToBackBufferProgram mainToBackBufferProgram;

    VisualDepthTextureProgram shadowDebugProgram;
    FullscreenProgram textureDebugProgram;

    private CTerrainGenerator m_TerrainVB;
    private SkyRoundGenerator m_SkyVB;
    private SkyRoundRenderer m_SkyRenderer;

    private GLFuncProvider gl;

    void onCreate(String shaderPath, String texturePath){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        try {
            loadTextures(texturePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        m_TerrainVB = new CTerrainGenerator();
        m_TerrainVB.initlize();

        m_SkyVB = new SkyRoundGenerator();
        m_SkyVB.initlize(sky_gridpoints, sky_texture_angle, m_TerrainVB.getTerrainParams().terrain_geometry_scale * m_TerrainVB.getTerrainParams().terrain_gridpoints);

        m_SkyRenderer = new SkyRoundRenderer();
        m_SkyRenderer.initlize(m_SkyVB, sky_texture);

        GLCheck.checkError();
        renderHeightfieldProgram = new IsRenderHeightfieldProgram(null, shaderPath);
        waterNormalmapCombineProgram = new IsWaterNormalmapCombineProgram(shaderPath);
        mainToBackBufferProgram = new IsMainToBackBufferProgram(shaderPath);GLCheck.checkError();

        try {
            shadowDebugProgram = new VisualDepthTextureProgram(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        GLCheck.checkError();
        textureDebugProgram = new FullscreenProgram();
        terrain_textures = new TextureSampler[12];
        terrain_textures[0] = new TextureSampler(m_TerrainVB.getHeightmapTexture().getTexture(), IsSamplers.g_SamplerLinearWrap);
        terrain_textures[1] = new TextureSampler(m_TerrainVB.getLayerdefTexture().getTexture(), IsSamplers.g_SamplerLinearWrap);
        terrain_textures[2] = new TextureSampler(0, IsSamplers.g_SamplerLinearMipmapWrap);
        terrain_textures[3] = new TextureSampler(rock_bump_texture.getTexture(), IsSamplers.g_SamplerLinearMipmapWrap);
        terrain_textures[4] = new TextureSampler(0, IsSamplers.g_SamplerLinearWrap);
        terrain_textures[5] = new TextureSampler(0, IsSamplers.g_SamplerAnisotropicWrap);
        terrain_textures[6] = new TextureSampler(0, IsSamplers.g_SamplerAnisotropicWrap);
        terrain_textures[7] = new TextureSampler(0, IsSamplers.g_SamplerAnisotropicWrap);
        terrain_textures[8] = new TextureSampler(0, IsSamplers.g_SamplerAnisotropicWrap);
        terrain_textures[9] = new TextureSampler(0, IsSamplers.g_SamplerAnisotropicWrap);
        terrain_textures[10] = new TextureSampler(0, IsSamplers.g_SamplerAnisotropicWrap);
        terrain_textures[11] = new TextureSampler(0, IsSamplers.g_SamplerDepthAnisotropic);
    }

    private void buildFramebuffer(FramebufferGL fbo, Texture2DDesc[] tex_descs, TextureAttachDesc[] attach_descs){
        fbo.bind();

        for(int i = 0; i < tex_descs.length;i++){
            fbo.addTexture2D(tex_descs[i], attach_descs[i]);
        }

        gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);
    }

    void onReshape(int width, int height){
        if(width <= 0 || height <= 0)
            return;

        if(backbufferWidth == width && backbufferHeight == height)
            return;
        // release previous framebuffers.
        releaseFrameBuffer();
        backbufferWidth = width;
        backbufferHeight = height;

//		FrameBufferBuilder builder = new FrameBufferBuilder();
//		TextureInfo tex_desc = builder.createColorTexture();
//		builder.setWidth((int) (backbufferWidth * main_buffer_size_multiplier));
//		builder.setHeight((int) (backbufferHeight * main_buffer_size_multiplier));
//		tex_desc.setInternalFormat(GL11.GL_RGBA8);
//		TextureInfo depth_desc = builder.getOrCreateDepthTexture();
//		depth_desc.setInternalFormat(FramebufferGL.FBO_DepthBufferType_TEXTURE_DEPTH32F);

        Texture2DDesc[] tex_descs = {
                new Texture2DDesc((int) (backbufferWidth * main_buffer_size_multiplier), (int) (backbufferHeight * main_buffer_size_multiplier), GLenum.GL_RGBA8),
                new Texture2DDesc((int) (backbufferWidth * main_buffer_size_multiplier), (int) (backbufferHeight * main_buffer_size_multiplier), GLenum.GL_DEPTH_COMPONENT32F),
        };

        final TextureAttachDesc default_desc = new TextureAttachDesc();

        TextureAttachDesc[] attach_descs = {
                default_desc,
                default_desc
        };

        main_color_framebuffer = new FramebufferGL();
        reflection_framebuffer = new FramebufferGL();
        refraction_framebuffer = new FramebufferGL();
        buildFramebuffer(main_color_framebuffer, tex_descs, attach_descs);
        buildFramebuffer(reflection_framebuffer, tex_descs, attach_descs);
        buildFramebuffer(refraction_framebuffer, tex_descs, attach_descs);

//		builder.setWidth(water_normalmap_resource_buffer_size_xy);
//		builder.setHeight(water_normalmap_resource_buffer_size_xy);
//		depth_desc.setInternalFormat(FramebufferGL.FBO_DepthBufferType_NONE);
        water_normalmap_framebuffer = new FramebufferGL();
        water_normalmap_framebuffer.bind();
        water_normalmap_framebuffer.addTexture2D(new Texture2DDesc(water_normalmap_resource_buffer_size_xy, water_normalmap_resource_buffer_size_xy, GLenum.GL_RGB8), default_desc);

//		builder.setWidth(shadowmap_resource_buffer_size_xy);
//		builder.setHeight(shadowmap_resource_buffer_size_xy);
//		depth_desc.setInternalFormat(FramebufferGL.FBO_DepthBufferType_TEXTURE_DEPTH32F);
//		builder.getColorTextures().clear();
        shadownmap_framebuffer = new FramebufferGL(/*builder*/);
        shadownmap_framebuffer.bind();
        shadownmap_framebuffer.addTexture2D(new Texture2DDesc(shadowmap_resource_buffer_size_xy,shadowmap_resource_buffer_size_xy, GLenum.GL_DEPTH_COMPONENT32F), default_desc);

        terrain_textures[4].textureID = water_normalmap_framebuffer.getAttachedTex(0).getTexture();  // color
        terrain_textures[11].textureID = shadownmap_framebuffer.getAttachedTex(0).getTexture();   // depth
    }

    void onDraw(IsParameters params){
        CTerrainGenerator.TerrainParams terrainParams = m_TerrainVB.getTerrainParams();
        params.g_HeightFieldSize = terrainParams.terrain_gridpoints*terrainParams.terrain_geometry_scale;
        // Stage 1. render the shadownmap first.
        if(params.g_Wireframe){
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_LINE);
        }

        GLCheck.checkError();
        renderShadowMap(params);

        GLCheck.checkError();

        // Stage 3. render the reflection mapping.
        renderReflection(params);

//        if(debug_reflection){
//            drawFullscreen(reflection_framebuffer.getAttachedTex(0).getTexture());
//            return;
//        }

        // Stage 4. render to the main_color_framebuffer.
        float terrain_minheight=m_TerrainVB.getTerrainParams().terrain_minheight;
        params.g_HalfSpaceCullSign = 1.0f;
        params.g_HalfSpaceCullPosition=terrain_minheight*2;
        renderMainColor(params);
        GLCheck.checkError();

        if(params.g_Wireframe){
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
        }

        // Stage 5. getting back to rendering to default buffer
        int texture = params.g_showReflection ? refraction_framebuffer.getAttachedTex(0).getTexture() : main_color_framebuffer.getAttachedTex(0).getTexture();
        drawFullscreen(texture);
    }

    void renderMainColor(IsParameters params){
        main_color_framebuffer.bind();
        main_color_framebuffer.setViewPort();

        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(clearColor));
        gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.0f));

        // drawing terrain to main buffer
        params.g_TerrainBeingRendered = 1.0f;
        params.g_SkipCausticsCalculation = 0;
        renderTerrain(params, true, false);

        // resolving main buffer color to refraction color resource
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, refraction_framebuffer.getFramebuffer());
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, main_color_framebuffer.getFramebuffer());
        gl.glReadBuffer(GLenum.GL_COLOR_ATTACHMENT0);
        gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);
        gl.glBlitFramebuffer(0, 0, main_color_framebuffer.getWidth(), main_color_framebuffer.getHeight(),
                0, 0, refraction_framebuffer.getWidth(), refraction_framebuffer.getHeight(),
                GLenum.GL_COLOR_BUFFER_BIT | GLenum.GL_DEPTH_BUFFER_BIT, GLenum.GL_NEAREST);

        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);

        // rebind the main_framebuffer.
        main_color_framebuffer.bind();

        // drawing water surface to main buffer
        params.g_TerrainBeingRendered = 0;
        if(params.g_RenderWater)
            renderWater(params);

        //drawing sky to main buffer
        renderSky(params);
    }

    void renderWater(IsParameters params){

    }

    void drawFullscreen(int textureId){
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, backbufferWidth, backbufferHeight);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT);
        gl.glDisable(GLenum.GL_DEPTH_TEST);

        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, textureId);
        gl.glBindSampler(0, IsSamplers.g_SamplerLinearClamp);

        textureDebugProgram.enable();
        gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
        gl.glBindSampler(0, 0);
    }

    void renderReflection(IsParameters params){
        reflection_framebuffer.bind();
        reflection_framebuffer.setViewPort();

        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(refractionClearColor));
        gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.0f));

        setupReflectionView(params);

        // drawing sky to reflection RT
        renderSky(params);

        // drawing terrain to reflection RT
        params.g_SkipCausticsCalculation = 1;
        renderTerrain(params, false, false);

        endReflection(params);
    }

    void setupReflectionView(IsParameters params){
        // Save the old value
        camera_position.set(params.g_CameraPosition);
        camera_direction.set(params.g_CameraDirection);
        camera_modelView.load(params.g_ModelViewMatrix);
        camera_mvp.load(params.g_ModelViewProjectionMatrix);
        camera_projection.load(params.g_Projection);

        Vector3f eyePoint = params.g_CameraPosition;
        Vector3f direction = params.g_CameraDirection;

        // make the camera below the water.
        eyePoint.y=-1.0f*eyePoint.y+1.0f;
//		lookAtPoint.y=-1.0f*lookAtPoint.y+1.0f;
        direction.y *= -1;

        Matrix4f modelView = params.g_ModelViewMatrix;
        Matrix4f viewProj = params.g_ModelViewProjectionMatrix;
        modelView.m01 *= -1;
        modelView.m21 *= -1;
        modelView.m12 *= -1;
        modelView.m31 = -modelView.m31 - 1;

        Vector4f plane = new Vector4f(0, 1, 0, 0.5f);
        Matrix4f mat = Matrix4f.invertRigid(modelView, null).transpose();
        // transform the plane to camera coordinates.
        Matrix4f.transform(mat, plane, plane);
        // construct the oblique projection with new near plane.
        Matrix4f.obliqueClipping(params.g_Projection, plane, params.g_Projection);
        Matrix4f.mul(params.g_Projection, modelView, viewProj);

        params.g_HalfSpaceCullSign = 1.0f;
        params.g_HalfSpaceCullPosition = -0.6f;
    }

    void endReflection(IsParameters params){
        params.g_CameraPosition.set(camera_position);
        params.g_CameraDirection.set(camera_direction);
        params.g_ModelViewMatrix.load(camera_modelView);
        params.g_ModelViewProjectionMatrix.load(camera_mvp);
        params.g_Projection.load(camera_projection);
    }

    void renderSky(IsParameters params){
        m_SkyRenderer.render(params.g_ModelViewProjectionMatrix, params.g_Wireframe);
    }

    void renderShadowMap(IsParameters params){
        shadownmap_framebuffer.bind();
        shadownmap_framebuffer.setViewPort();

        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT);
        gl.glColorMask(false, false, false, false);  // disable color output.

        setupLightView(params);
        params.g_Wireframe = true; // Enable this to get better performance.
        renderTerrain(params, false, true);
        gl.glColorMask(true, true, true, true);
        params.g_Wireframe = false;
        GLCheck.checkError();
    }

    private void setupLightView(IsParameters params){
        float terrain_far_range = m_TerrainVB.getTerrainParams().terrain_geometry_scale * m_TerrainVB.getTerrainParams().terrain_gridpoints;
        Matrix4f worldToLightSpace =  params.g_LightModelViewProjectionMatrix;
        Vector3f EyePoint = params.g_LightPosition;
        EyePoint.set(14000.0f,6500.0f,4000.0f);
        float distanceFromLightToTarget = EyePoint.length();

//        XMVECTOR LookAtPoint  = XMVectorSet(terrain_far_range / 2.0f, 0.0f, terrain_far_range / 2.0f, 0);
        Vector3f LookAtPoint =params.g_LightTarget;
        LookAtPoint.set(terrain_far_range / 2.0f, 0.0f, terrain_far_range / 2.0f);
        ReadableVector3f lookUp = Vector3f.Y_AXIS;
        Vector3f cameraPosition = params.g_CameraPosition;

        float nr, fr;
        nr=distanceFromLightToTarget-terrain_far_range*1.0f;
        fr=distanceFromLightToTarget+terrain_far_range*1.0f;

//        XMMATRIX mView = XMMatrixLookAtLH(EyePoint, LookAtPoint, lookUp); // *D3DXMatrixLookAtLH(&mView, &EyePoint, &LookAtPoint, &lookUp);
//        XMMATRIX mProjMatrix = XMMatrixOrthographicLH(terrain_far_range*1.5, terrain_far_range, nr, fr); //*D3DXMatrixOrthoLH(&mProjMatrix, terrain_far_range*1.5, terrain_far_range, nr, fr);
//        XMMATRIX mViewProj = mView * mProjMatrix;
//        XMMATRIX mViewProjInv;

        Matrix4f mView = camera_modelView;
        Matrix4f.lookAt(EyePoint, LookAtPoint, lookUp, mView);
        Matrix4f.ortho(0.0f, terrain_far_range*1.5f, 0.0f, terrain_far_range, nr, fr, worldToLightSpace);
        Matrix4f.mul(worldToLightSpace, mView, worldToLightSpace);
//
//        mViewProjInv = XMMatrixInverse(NULL, mViewProj);

//        XMFLOAT4X4 vpStore, vpiStore;
//        XMStoreFloat4x4(&vpStore, mViewProj);
//        XMStoreFloat4x4(&vpiStore, mViewProjInv);
//        XMFLOAT4 camStore, ndStore;
//        XMStoreFloat4(&camStore, cameraPosition);

//        ID3DX11Effect* oceanFX = g_pOceanSurf->m_pOceanFX;
//        oceanFX->GetVariableByName("g_LightModelViewProjectionMatrix")->AsMatrix()->SetMatrix((FLOAT*)&vpStore);
//
//        pEffect->GetVariableByName("g_ModelViewProjectionMatrix")->AsMatrix()->SetMatrix((FLOAT*)&vpStore);
//        pEffect->GetVariableByName("g_LightModelViewProjectionMatrix")->AsMatrix()->SetMatrix((FLOAT*)&vpStore);
//        pEffect->GetVariableByName("g_LightModelViewProjectionMatrixInv")->AsMatrix()->SetMatrix((FLOAT*)&vpiStore);
//        pEffect->GetVariableByName("g_CameraPosition")->AsVector()->SetFloatVector((FLOAT*)&camStore);
//
//        XMVECTOR normalized_direction = XMVector3Normalize(cam->GetLookAtPt() - cam->GetEyePt());
//        XMStoreFloat4(&ndStore, normalized_direction);
//
//        pEffect->GetVariableByName("g_CameraDirection")->AsVector()->SetFloatVector((FLOAT*)&ndStore);
//
//        pEffect->GetVariableByName("g_HalfSpaceCullSign")->AsScalar()->SetFloat(1.0);
//        pEffect->GetVariableByName("g_HalfSpaceCullPosition")->AsScalar()->SetFloat(terrain_minheight*2);

        final float terrain_minheight = m_TerrainVB.getTerrainParams().terrain_minheight;
        params.g_HalfSpaceCullSign = 1.0f;
        params.g_HalfSpaceCullPosition = terrain_minheight*2;
        params.g_TerrainBeingRendered = 1;
        params.g_SkipCausticsCalculation = 1;
    }

    void renderTerrain(IsParameters params, boolean cullface, boolean shadow_map){

        renderHeightfieldProgram.enable(params, terrain_textures);
        if(params.g_Wireframe){
            renderHeightfieldProgram.setupColorPass();
        }else{
            renderHeightfieldProgram.setupRenderHeightFieldPass();
        }

        renderHeightfieldProgram.setRenderShadowmap(shadow_map);
        m_TerrainVB.draw(0, cullface);
        renderHeightfieldProgram.disable();
    }

    void releaseFrameBuffer(){
        if(main_color_framebuffer != null){
            main_color_framebuffer.dispose();
            main_color_framebuffer = null;
        }

        if(reflection_framebuffer != null){
            reflection_framebuffer.dispose();
            reflection_framebuffer = null;
        }

        if(refraction_framebuffer != null){
            refraction_framebuffer.dispose();
            refraction_framebuffer = null;
        }

        if(shadownmap_framebuffer != null){
            shadownmap_framebuffer.dispose();
            shadownmap_framebuffer = null;
        }

        if(water_normalmap_framebuffer != null){
            water_normalmap_framebuffer.dispose();
            water_normalmap_framebuffer = null;
        }
    }

    void onDestroy(){
        releaseFrameBuffer();
    }

    void loadTextures(String prefix)throws IOException{
        NvImage.upperLeftOrigin(false);
        rock_bump_texture = wrap(NvImage.uploadTextureFromDDSFile(prefix + "rock_bump6.dds"));
        sky_texture = wrap(NvImage.uploadTextureFromDDSFile(prefix + "sky.dds"));
        foam_intensity_perlin2 = wrap(NvImage.uploadTextureFromDDSFile(prefix + "foam_intensity_perlin2.dds"));
        foam24bit = wrap(NvImage.uploadTextureFromDDSFile(prefix + "foam24bit.dds"));

        GLCheck.checkError();

        gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
    }

    static Texture2D wrap(int textureID){
        return TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, textureID);
    }
}