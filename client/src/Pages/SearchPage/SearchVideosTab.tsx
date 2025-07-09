import React, { useEffect, useState } from "react";
import { SearchVideosMessage } from "Plugins/RecommendationService/APIs/SearchVideosMessage";
import { Video } from "Plugins/VideoService/Objects/Video";
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { useNavigateHome, useNavigateVideo } from "Globals/Navigate";
import { useUserToken } from "Globals/GlobalStore";
import { formatTime } from "Components/Formatter";
import { VideoStatus } from "Plugins/VideoService/Objects/VideoStatus";

interface VideoWithUploader {
    video: Video;
    uploaderInfo: UserInfo;
}

const SearchVideosTab: React.FC<{ keyword: string }> = ({ keyword }) => {
    const [videoResults, setVideoResults] = useState<VideoWithUploader[]>([]);
    const [loading, setLoading] = useState(true);
    const { navigateHome } = useNavigateHome();
    const { navigateVideo } = useNavigateVideo();
    const userToken = useUserToken();

    const fetchSearchResults = async (keyword: string) => {
        try {
            if (!keyword) {
                setVideoResults([]);
                return;
            }
            setLoading(true);
            const videos = await new Promise<Video[]>((resolve, reject) => {
                new SearchVideosMessage(userToken ? userToken : null, keyword, 30).send(
                    (info: string) => resolve(JSON.parse(info) as Video[]),
                    (error: string) => reject(new Error(error))
                );
            });
            const videosWithUploader = await Promise.all(
                videos.map(async (video) => {
                    const uploaderInfo = await new Promise<UserInfo>((resolve, reject) => {
                        new QueryUserInfoMessage(video.uploaderID).send(
                            (info: string) => resolve(JSON.parse(info) as UserInfo),
                            (err: string) => reject(new Error(err))
                        );
                    });
                    return { video, uploaderInfo };
                })
            );
            setVideoResults(videosWithUploader);
        } catch (error) {
            console.error("搜索视频失败:", error);
            setVideoResults([]);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        fetchSearchResults(keyword);
    }, [keyword]);

    if (loading) {
        return <div style={{ textAlign: "center", color: "#888", padding: "32px 0" }}>加载中...</div>;
    }
    if (!loading && videoResults.length === 0) {
        return <div style={{ textAlign: "center", color: "#aaa", padding: "32px 0" }}>暂无相关视频</div>;
    }

    return (
        <>
            {videoResults.map(video => (
                <div className="search-video-item" key={video.video.videoID}>
                    <img
                        className="search-video-cover"
                        src={video.video.cover || require("Images/DefaultCover.jpg")}
                        alt="封面"
                        onClick={() => navigateVideo(video.video.videoID)}
                    />
                    <div className="search-video-info">
                        <div>
                            <span className="search-video-title" onClick={() => navigateVideo(video.video.videoID)}>
                                {video.video.title}
                            </span>
                        </div>
                        <div className="search-video-tags">
                            {video.video.tag && video.video.tag.map((t, i) => (
                                <span className="search-video-tag" key={i}>{t}</span>
                            ))}
                        </div>
                        <div className="search-video-meta">
                            {video.uploaderInfo && (
                                <span className="search-video-uploader" onClick={() => navigateHome(video.uploaderInfo.userID)}>
                                    <img
                                        className="search-video-uploader-avatar"
                                        src={video.uploaderInfo.avatarPath}
                                        alt="UP主"
                                    />
                                    <span>{video.uploaderInfo.username}</span>
                                </span>
                            )}
                            <span>发布时间：{formatTime(video.video.uploadTime)}</span>
                        </div>
                        <div className="search-video-desc">{video.video.description}</div>
                    </div>
                </div>
            ))}
        </>
    );
};

export default SearchVideosTab;
