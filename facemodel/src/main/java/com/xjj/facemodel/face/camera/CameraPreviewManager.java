package com.xjj.facemodel.face.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import com.xjj.facemodel.face.callback.CameraDataCallback;
import com.xjj.facemodel.face.model.SingleBaseConfig;
import com.xjj.facemodel.face.utils.BitmapUtils;

import java.util.List;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.RIGHT;
import static android.view.Gravity.TOP;

/**
 * Time: 2019/1/24
 * Author: v_chaixiaogang
 * Description:
 */
public class CameraPreviewManager implements TextureView.SurfaceTextureListener {

    private static final String TAG = "camera_preview";


    AutoTexturePreviewView mTextureView;
    boolean mPreviewed = false;
    private boolean mSurfaceCreated = false;
    private SurfaceTexture mSurfaceTexture;

    public static final int CAMERA_FACING_BACK = 0;

    public static final int CAMERA_FACING_FRONT = 1;

    public static final int CAMERA_USB = 2;

    public static final int CAMERA_ORBBEC = 3;

    /**
     * 垂直方向
     */
    public static final int ORIENTATION_PORTRAIT = 0;
    /**
     * 水平方向
     */
    public static final int ORIENTATION_HORIZONTAL = 1;

    /**
     * 当前相机的ID。
     */
    private int cameraFacing = CAMERA_FACING_FRONT;

    private int previewWidth;
    private int previewHeight;

    private int videoWidth;
    private int videoHeight;

    private int tempWidth;
    private int tempHeight;

    private int textureWidth;
    private int textureHeight;

    private Camera mCamera;
    private int mCameraNum;

    private int displayOrientation = 0;
    private int cameraId = 0;
    private int mirror = 1; // 镜像处理
    private CameraDataCallback mCameraDataCallback;
    private static volatile CameraPreviewManager instance = null;
    private int mCurrentCameraFacing;//当前摄像头
    private OnCameraActionCallback mOnCameraActionCallback;
    private byte[] mCameraData;//拍照返回的图像数据
    private ToneGenerator mToneGenerator;
    private int mDeviceOrientation = TOP;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {

        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    public static CameraPreviewManager getInstance() {
        synchronized (CameraPreviewManager.class) {
            if (instance == null) {
                instance = new CameraPreviewManager();
            }
        }
        return instance;
    }

    public int getCameraFacing() {
        return cameraFacing;
    }

    public void setCameraFacing(int cameraFacing) {
        this.cameraFacing = cameraFacing;
    }

    public int getDisplayOrientation() {
        return displayOrientation;
    }

    public void setDisplayOrientation(int displayOrientation) {
        this.displayOrientation = displayOrientation;
    }

    /**
     * 开启预览
     *
     * @param context
     * @param textureView
     */
    public void startPreview(Context context, AutoTexturePreviewView textureView, int width,
                             int height, CameraDataCallback cameraDataCallback) {
        Log.e(TAG, "开启预览模式");
        Context mContext = context;
        this.mCameraDataCallback = cameraDataCallback;
        mTextureView = textureView;
        this.previewWidth = width;
        this.previewHeight = height;
        mSurfaceTexture = mTextureView.getTextureView().getSurfaceTexture();
        mTextureView.getTextureView().setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int i, int i1) {
        Log.e(TAG, "--surfaceTexture--SurfaceTextureAvailable");
        mSurfaceTexture = texture;
        mSurfaceCreated = true;
        textureWidth = i;
        textureHeight = i1;
        openCamera();

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int i, int i1) {
        Log.e(TAG, "--surfaceTexture--TextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
        Log.e(TAG, "--surfaceTexture--destroyed");
        mSurfaceCreated = false;
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        // Log.e(TAG, "--surfaceTexture--Updated");
    }


    /**
     * 关闭预览
     */
    public void stopPreview() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(null);
                mSurfaceCreated = false;
                mTextureView = null;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                Log.e("qing", "camera destory error");
                e.printStackTrace();

            }
        }
    }


    /**
     * 开启摄像头
     */

    private void openCamera() {

        try {
            if (mCamera == null) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == cameraFacing) {
                        cameraId = i;
                    }
                }
                mCamera = Camera.open(cameraId);
                Log.e(TAG, "initCamera---open camera");
            }

            // 摄像头图像预览角度
            int cameraRotation = SingleBaseConfig.getBaseConfig().getVideoDirection();
            switch (cameraFacing) {
                case CAMERA_FACING_FRONT: {
//                    cameraRotation = ORIENTATIONS.get(displayOrientation);
//                    cameraRotation = getCameraDisplayOrientation(cameraRotation, cameraId);
                    mCamera.setDisplayOrientation(cameraRotation);
                    break;
                }

                case CAMERA_FACING_BACK: {
//                    cameraRotation = ORIENTATIONS.get(displayOrientation);
//                    cameraRotation = getCameraDisplayOrientation(cameraRotation, cameraId);
                    mCamera.setDisplayOrientation(cameraRotation);
                    break;
                }

                case CAMERA_USB: {
                    mCamera.setDisplayOrientation(cameraRotation);
                    break;
                }

                default:
                    break;

            }


            if (cameraRotation == 90 || cameraRotation == 270) {
                int isRgbRevert = SingleBaseConfig.getBaseConfig().getMirrorRGB();
                if (isRgbRevert == 1) {
                    mTextureView.setRotationY(180);
                } else {
                    mTextureView.setRotationY(0);
                }
                // 旋转90度或者270，需要调整宽高
                mTextureView.setPreviewSize(previewHeight, previewWidth);
            } else {
                int isRgbRevert = SingleBaseConfig.getBaseConfig().getMirrorRGB();
                if (isRgbRevert == 1) {
                    mTextureView.setRotationY(180);
                } else {
                    mTextureView.setRotationY(0);
                }
                mTextureView.setPreviewSize(previewWidth, previewHeight);
            }
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> sizeList = params.getSupportedPreviewSizes(); // 获取所有支持的camera尺寸
            final Camera.Size optionSize = getOptimalPreviewSize(sizeList, previewWidth,
                    previewHeight); // 获取一个最为适配的camera.size
            if (optionSize.width == previewWidth && optionSize.height == previewHeight) {
                videoWidth = previewWidth;
                videoHeight = previewHeight;
            } else {
                videoWidth = optionSize.width;
                videoHeight = optionSize.height;
            }
            params.setPreviewSize(videoWidth, videoHeight);

            mCamera.setParameters(params);
            try {
                mCamera.setPreviewTexture(mSurfaceTexture);
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] bytes, Camera camera) {
                        if (mCameraDataCallback != null) {
                            mCameraDataCallback.onGetCameraData(bytes, camera,
                                    videoWidth, videoHeight);
                        }
                    }
                });
                mCamera.startPreview();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        } catch (RuntimeException e) {
            Log.e(TAG, e.getMessage());
        }
    }


    private int getCameraDisplayOrientation(int degrees, int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation + degrees) % 360;
            rotation = (360 - rotation) % 360; // compensate the mirror
        } else { // back-facing
            rotation = (info.orientation - degrees + 360) % 360;
        }
        return rotation;
    }


    /**
     * 解决预览变形问题
     *
     * @param sizes
     * @param w
     * @param h
     * @return
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double aspectTolerance = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) {
            return null;
        }
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > aspectTolerance) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
    public interface OnCameraActionCallback {
        void onTakePictureComplete(Bitmap bitmap);
    }


    /**
     * 拍照
     */
    public void takePicture(OnCameraActionCallback callback) {
        mOnCameraActionCallback = callback;
        if (mCamera != null) {
            mCamera.takePicture(null, null, mJpegPictureCallback);
        }
    }


    //快门按下的回调，在这里我们可以设置类似播放“咔嚓”声之类的操作。默认的就是咔嚓。
    private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback() {

        public void onShutter() {
            // TODO Auto-generated method stub
            if (mToneGenerator == null) {
                //发出提示用户的声音
                mToneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC,
                        ToneGenerator.MAX_VOLUME);
            }
            mToneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2);
        }
    };

    //对jpeg图像数据的回调
    private Camera.PictureCallback mJpegPictureCallback = new Camera.PictureCallback() {

        public void onPictureTaken(byte[] data, Camera camera) {
            //mCameraData = data;
            if (mOnCameraActionCallback != null) {
                mCamera.cancelAutoFocus();
                //恢复对焦模式
                mCamera.getParameters().setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mCamera.getParameters().setFocusAreas(null);
                mCamera.setParameters(mCamera.getParameters());
                mCamera.startPreview();
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);//data是字节数据，将其解析成位图
                if (mCurrentCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    if (mDeviceOrientation == TOP) {
                        bitmap = BitmapUtils.rotateNewBitmap(bitmap, 90.0f);
                    } else if (mDeviceOrientation == RIGHT) {
                        bitmap = BitmapUtils.rotateNewBitmap(bitmap, 180.0f);
                    } else if (mDeviceOrientation == BOTTOM) {
                        bitmap = BitmapUtils.rotateNewBitmap(bitmap, 270.0f);
                    }
                } else if (mCurrentCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    if (mDeviceOrientation == TOP) {
                        bitmap = BitmapUtils.rotateNewBitmap(bitmap, 270.0f);
                    } else if (mDeviceOrientation == RIGHT) {
                        bitmap = BitmapUtils.rotateNewBitmap(bitmap, 180.0f);
                    } else if (mDeviceOrientation == BOTTOM) {
                        bitmap = BitmapUtils.rotateNewBitmap(bitmap, 90.0f);
                    }
                }
                mOnCameraActionCallback.onTakePictureComplete(bitmap);
            }
        }
    };


}
