package jet.opengl.demos.intel.va;

import com.sun.javaws.Globals;

import javafx.scene.Camera;
import javafx.scene.effect.Lighting;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaDrawContext {
    // vaRenderDeviceContext is used to get/set current render targets and access rendering API stuff like contexts, etc.
//    vaRenderDeviceContext &         APIContext;
    public VaCameraBase             Camera;             // Currently selected camera
    public VaRenderingGlobals            Globals;            // Used to set global shader constants, track current frame index, provide some debugging tools, etc.
    public vaLighting Lighting;
    void * const                    UserContext;

    // can be changed at runtime
    vaRenderPassType                PassType;
    class vaSimpleShadowMap *       SimpleShadowMap;
}
