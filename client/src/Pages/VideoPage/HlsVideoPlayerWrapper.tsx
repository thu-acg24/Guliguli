import React, { useState, useEffect } from "react";
import MinioVideoPlayer from "./HlsVideoPlayer";
import { useUserToken } from "Globals/GlobalStore";
import { QueryM3U8PathMessage } from "Plugins/VideoService/APIs/QueryM3U8PathMessage";
import { Video } from 'Plugins/VideoService/Objects/Video';

interface HlsVideoPlayerWrapperProps {
  videoID: number;
  videoinfo:Video;
}

const HlsVideoPlayerWrapper: React.FC<HlsVideoPlayerWrapperProps> = ({ videoID,videoinfo }) => {
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const userToken = useUserToken();

  useEffect(() => {
    const fetchVideoPath = async () => {
      try {
        setLoading(true);
        setError(null);
        
        const uploadPath = await new Promise<string>((resolve, reject) => {
          new QueryM3U8PathMessage(userToken?userToken:null, videoID).send(
            (info: string) => {
              try {
                // 假设返回的是直接可用的播放路径
                const data: string = JSON.parse(info);
                resolve(data);
              } catch (e) {
                reject(new Error("Failed to parse video path"));
              }
            },
            (errorMsg) => reject(new Error(errorMsg))
          );
        });
        
        setVideoUrl(uploadPath);
      } catch (err) {
        console.error("获取视频路径失败:", err);
        setError("无法加载视频，请稍后再试");
      } finally {
        setLoading(false);
      }
    };

    if (videoID) {
      fetchVideoPath();
    } else {
      setError("无效的视频ID");
      setLoading(false);
    }
  }, [videoID]);

  if (loading) {
    return (
      <div className="video-player-loading">
        <div className="loader"></div>
        <p>加载视频中...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="video-player-error">
        <p>⚠️ {error}</p>
      </div>
    );
  }

  if (!videoUrl) {
    return (
      <div className="video-player-error">
        <p>无法获取视频地址</p>
      </div>
    );
  }

  return <MinioVideoPlayer videoUrl={videoUrl}videoinfo={videoinfo}/>;
};

export default HlsVideoPlayerWrapper;