/**
 * 从视频文件中提取第一帧作为封面
 */
export const extractFirstFrameFromVideo = (videoFile: File): Promise<File> => {
    return new Promise((resolve, reject) => {
        const video = document.createElement('video');
        const canvas = document.createElement('canvas');
        const context = canvas.getContext('2d');

        if (!context) {
            reject(new Error('无法创建 canvas 上下文'));
            return;
        }

        video.addEventListener('loadedmetadata', () => {
            // 设置 canvas 尺寸
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            
            // 跳转到第一帧
            video.currentTime = 0;
        });

        video.addEventListener('seeked', () => {
            try {
                // 绘制当前帧到 canvas
                context.drawImage(video, 0, 0, canvas.width, canvas.height);
                
                // 将 canvas 转换为 Blob
                canvas.toBlob((blob) => {
                    if (blob) {
                        // 创建 File 对象
                        const frameFile = new File([blob], 'video-frame.jpg', {
                            type: 'image/jpeg',
                            lastModified: Date.now(),
                        });
                        resolve(frameFile);
                    } else {
                        reject(new Error('无法从视频中提取帧'));
                    }
                }, 'image/jpeg', 0.8); // 使用 JPEG 格式，质量为 80%
            } catch (error) {
                reject(error);
            } finally {
                // 清理资源
                URL.revokeObjectURL(video.src);
            }
        });

        video.addEventListener('error', (error) => {
            reject(new Error('视频加载失败'));
        });

        // 设置视频源
        video.src = URL.createObjectURL(videoFile);
        video.load();
    });
};
