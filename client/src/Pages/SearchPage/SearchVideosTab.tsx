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
        // fetchSearchResults(keyword);
        // mock 数据
        const mockVideoResults: VideoWithUploader[] = [
            {
                video: new Video(
                    1,
                    "Mock 视频标题 1",
                    "这是一个用于测试的 mock 视频描述。",
                    120,
                    require("Images/DefaultCover.jpg"),
                    ["搞笑", "测试"],
                    101,
                    1234,
                    56,
                    12,
                    VideoStatus.approved,
                    Date.now() - 10000000
                ),
                uploaderInfo: new UserInfo(
                    101,
                    "MockUP主1",
                    require("Images/DefaultAvatar.jpg"),
                    false,
                    "Mock 简介 1"
                )
            },
            {
                video: new Video(
                    2,
                    "Mock 视频标题 2",
                    "第二个 mock 视频描述。",
                    300,
                    require("Images/DefaultCover.jpg"),
                    ["音乐"],
                    102,
                    5678,
                    78,
                    34,
                    VideoStatus.approved,
                    Date.now() - 5000000
                ),
                uploaderInfo: new UserInfo(
                    102,
                    "MockUP主2",
                    require("Images/DefaultAvatar.jpg"),
                    false,
                    "Mock 简介 2"
                )
            },
            {
                video: new Video(
                    3,
                    "Mock 视频标题 3",
                    "第三个 mock 视频描述。",
                    240,
                    require("Images/DefaultCover.jpg"),
                    ["科技"],
                    103,
                    8910,
                    102,
                    45,
                    VideoStatus.approved,
                    Date.now() - 8000000
                ),
                uploaderInfo: new UserInfo(
                    103,
                    "MockUP主3",
                    require("Images/DefaultAvatar.jpg"),
                    false,
                    "Mock 简介 3"
                )
            },
            {
                video: new Video(
                    4,
                    "Mock 视频标题 4",
                    "第四个 mock 视频描述。",
                    180,
                    require("Images/DefaultCover.jpg"),
                    ["游戏", "解说"],
                    104,
                    4321,
                    87,
                    23,
                    VideoStatus.approved,
                    Date.now() - 2000000
                ),
                uploaderInfo: new UserInfo(
                    104,
                    "MockUP主4",
                    require("Images/DefaultAvatar.jpg"),
                    false,
                    "Mock 简介 4"
                )
            },
            {
                video: new Video(
                    5,
                    "Mock 视频标题 5",
                    "第五个 mock 视频描述。",
                    60,
                    require("Images/DefaultCover.jpg"),
                    ["生活"],
                    105,
                    2468,
                    43,
                    9,
                    VideoStatus.approved,
                    Date.now() - 600000
                ),
                uploaderInfo: new UserInfo(
                    105,
                    "MockUP主5",
                    require("Images/DefaultAvatar.jpg"),
                    false,
                    "Mock 简介 5"
                )
            },
            {
                video: new Video(
                    6,
                    "Mock 视频标题 6",
                    "第六个 mock 视频描述。",
                    150,
                    require("Images/DefaultCover.jpg"),
                    ["旅行", "风景"],
                    106,
                    3691,
                    55,
                    18,
                    VideoStatus.approved,
                    Date.now() - 3000000
                ),
                uploaderInfo: new UserInfo(
                    106,
                    "MockUP主6",
                    require("Images/DefaultAvatar.jpg"),
                    false,
                    "Mock 简介 6"
                )
            },
            {
                video: new Video(
                    7,
                    "Mock 视频标题 7",
                    "第七个 mock 视频描述。",
                    210,
                    require("Images/DefaultCover.jpg"),
                    ["教育", "学习"],
                    107,
                    7531,
                    99,
                    30,
                    VideoStatus.approved,
                    Date.now() - 7000000
                ),
                uploaderInfo: new UserInfo(
                    107,
                    "MockUP主7",
                    require("Images/DefaultAvatar.jpg"),
                    false,
                    "Mock 简介 7"
                )
            },
            {
                video: new Video(
                    8,
                    "Mock 视频标题 8",
                    "第八个 mock 视频描述。",
                    95,
                    require("Images/DefaultCover.jpg"),
                    ["美食"],
                    108,
                    1823,
                    22,
                    5,
                    VideoStatus.approved,
                    Date.now() - 1200000
                ),
                uploaderInfo: new UserInfo(
                    108,
                    "MockUP主8",
                    require("Images/DefaultAvatar.jpg"),
                    false,
                    "Mock 简介 8"
                )
            },
            {
                video: new Video(
                    9,
                    "Mock 视频标题 9",
                    "第九个 mock 视频描述。",
                    400,
                    require("Images/DefaultCover.jpg"),
                    ["纪录片"],
                    109,
                    9876,
                    120,
                    67,
                    VideoStatus.approved,
                    Date.now() - 9500000
                ),
                uploaderInfo: new UserInfo(
                    109,
                    "MockUP主9",
                    require("Images/DefaultAvatar.jpg"),
                    false,
                    "Mock 简介 9"
                )
            },
            {
                video: new Video(
                    10,
                    "Mock 视频标题 10",
                    "第十个 mock 视频描述。",
                    360,
                    require("Images/DefaultCover.jpg"),
                    ["体育", "篮球"],
                    110,
                    6543,
                    88,
                    29,
                    VideoStatus.approved,
                    Date.now() - 4000000
                ),
                uploaderInfo: new UserInfo(
                    110,
                    "MockUP主10",
                    require("Images/DefaultAvatar.jpg"),
                    false,
                    "Mock 简介 10"
                )
            }
        ];
        setVideoResults(mockVideoResults);
        setLoading(false);
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
