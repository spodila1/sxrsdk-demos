/* Copyright 2016 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf.ply;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.gearvrf.GVRCameraRig;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRLight;
import org.gearvrf.GVRMain;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRRenderPass;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRScreenshot3DCallback;
import org.gearvrf.GVRScreenshotCallback;
import org.gearvrf.GVRShader;
import org.gearvrf.GVRShaderId;
import org.gearvrf.GVRTransform;
import org.gearvrf.SystemPropertyUtil;
import org.gearvrf.utility.Threads;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author m1.williams
 * PLY Demo comes with test files in the assets directory:
 *
 *         In the line: "String filename = ", change to one of the above file
 *         names or supply you own.
 *
 */

public class PLYparserScript extends GVRMain
{

  private static final String TAG = PLYparserScript.class.getSimpleName();
  private GVRContext mGVRContext = null;
  GVRScene scene = null;

  public PLYparserScript(PLYparserActivity activity)
  {
  }

  public void onInit(GVRContext gvrContext)
  {
    mGVRContext = gvrContext;

    scene = gvrContext.getMainScene();
    scene.setBackgroundColor(.7f, .7f, .8f, 1);

    GVRSceneObject model = new GVRSceneObject(mGVRContext);
    // PLY test files should be in the assets directory.
    // Replace 'filename' to view another .ply file
    String filename = SystemPropertyUtil.getSystemPropertyString("debug.gearvrf.ply");
    if (null == filename) {
      filename = "colorCube.ply";
    }
    filename = "JasonM_tris_ascii.ply";
    try
    {
      model = gvrContext.getAssetLoader().loadModel(filename, scene);

      GVRTransform transform = model.getTransform();
      transform.setRotationByAxis(-90, 1, 0, 0);

      GVRCameraRig mainCameraRig = gvrContext.getMainScene().getMainCameraRig();
      mainCameraRig.getTransform().setPosition(0, 2.75f,2.25f);

      List<GVRRenderData> rdatas = model.getAllComponents(GVRRenderData.getComponentType());
      for (GVRRenderData rdata : rdatas)
      {
        rdata.setCullFace( GVRRenderPass.GVRCullFaceEnum.None );
      }

      // check if a headlight was attached to the model's camera rig
      // during parsing, as specified by the NavigationInfo node.
      // If 4 objects are attached to the camera rig, one must be the
      // directionalLight. Thus attach a dirLight to the main camera
      if (GVRShader.isVulkanInstance()) // remove light on Vulkan
      {
        List<GVRLight> lights = model.getAllComponents(GVRLight.getComponentType());
        for (GVRLight l : lights)
        {
          GVRSceneObject owner = l.getOwnerObject();
          owner.getParent().removeChildObject(owner);
        }
      }
    }
    catch (FileNotFoundException e)
    {
      Log.d(TAG, "ERROR: FileNotFoundException: " + filename);
    }
    catch (IOException e)
    {
      Log.d(TAG, "Error IOException = " + e);
      e.printStackTrace();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    mGVRContext.getInputManager().selectController();
  } // end onInit()

  // @Override
  public void onStep()
  {
    FPSCounter.tick();
  }

  private boolean lastScreenshotLeftFinished = true;
  private boolean lastScreenshotRightFinished = true;
  private boolean lastScreenshotCenterFinished = true;
  private boolean lastScreenshot3DFinished = true;

  // mode 0: center eye; mode 1: left eye; mode 2: right eye
  public void captureScreen(final int mode, final String filename)
  {
    Threads.spawn(new Runnable()
    {
      public void run()
      {

        switch (mode)
        {
          case 0:
            if (lastScreenshotCenterFinished)
            {
              mGVRContext
                      .captureScreenCenter(newScreenshotCallback(filename, 0));
              lastScreenshotCenterFinished = false;
            }
            break;
          case 1:
            if (lastScreenshotLeftFinished)
            {
              mGVRContext.captureScreenLeft(newScreenshotCallback(filename, 1));
              lastScreenshotLeftFinished = false;
            }
            break;
          case 2:
            if (lastScreenshotRightFinished)
            {
              mGVRContext
                      .captureScreenRight(newScreenshotCallback(filename, 2));
              lastScreenshotRightFinished = false;
            }
            break;
        }
      }
    });
  }

  public void captureScreen3D(String filename)
  {
    if (lastScreenshot3DFinished)
    {
      mGVRContext.captureScreen3D(newScreenshot3DCallback(filename));
      lastScreenshot3DFinished = false;
    }
  }

  private GVRScreenshotCallback newScreenshotCallback(final String filename,
                                                      final int mode)
  {
    return new GVRScreenshotCallback()
    {

      @Override
      public void onScreenCaptured(Bitmap bitmap)
      {
        if (bitmap != null)
        {
          File file = new File(Environment.getExternalStorageDirectory(),
                  filename + ".png");
          FileOutputStream outputStream = null;
          try
          {
            outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
          }
          catch (FileNotFoundException e)
          {
            e.printStackTrace();
          }
          finally
          {
            try
            {
              outputStream.close();
            }
            catch (IOException e)
            {
              e.printStackTrace();
            }
          }
        }
        else
        {
          Log.e("SampleActivity", "Returned Bitmap is null");
        }

        // enable next screenshot
        switch (mode)
        {
          case 0:
            lastScreenshotCenterFinished = true;
            break;
          case 1:
            lastScreenshotLeftFinished = true;
            break;
          case 2:
            lastScreenshotRightFinished = true;
            break;
        }
      }
    };
  }

  private GVRScreenshot3DCallback newScreenshot3DCallback(final String filename)
  {
    return new GVRScreenshot3DCallback()
    {

      @Override
      public void onScreenCaptured(Bitmap[] bitmapArray)
      {
        Log.d("SampleActivity", "Length of bitmapList: " + bitmapArray.length);
        if (bitmapArray.length > 0)
        {
          for (int i = 0; i < bitmapArray.length; i++)
          {
            Bitmap bitmap = bitmapArray[i];
            File file = new File(Environment.getExternalStorageDirectory(),
                    filename + "_" + i + ".png");
            FileOutputStream outputStream = null;
            try
            {
              outputStream = new FileOutputStream(file);
              bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            }
            catch (FileNotFoundException e)
            {
              e.printStackTrace();
            }
            finally
            {
              try
              {
                outputStream.close();
              }
              catch (IOException e)
              {
                e.printStackTrace();
              }
            }
          }
        }
        else
        {
          Log.e("SampleActivity", "Returned Bitmap List is empty");
        }

        // enable next screenshot
        lastScreenshot3DFinished = true;
      }
    };
  }
}