import { GetRecommendedVideosMessage } from "Plugins/RecommendationService/APIs/GetRecommendedVideosMessage";
import { fetchOtherUserInfo } from "Globals/UserService";
import DefaultCover from "Images/DefaultCover.jpg";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";

// 简化的 Video 类型
export interface SimpleVideo {
  videoID: number;
  title: string;
  cover: string;
  duration: number;
  description: string;
  uploaderID: number;
  likes: number;
  favorites: number;
  views: number;
  uploadTime: string;
  uploaderInfo?: UserInfo;
}

/**
 * 获取推荐视频列表（自动获取UP主信息）
 * @param userToken 用户token
 * @param videoID 视频id
 * @param count 要获取的视频数量（默认15）
 * @returns Promise<SimpleVideo[]>
 */
export async function getRecommendedVideos(
  userToken: string | null,
  videoID: number | null,
  count: number = 15
): Promise<SimpleVideo[]> {
  return new Promise((resolve, reject) => {
    new GetRecommendedVideosMessage(videoID, userToken, 0.2, count).send(
      async (info: string) => {
        try {
          const data: SimpleVideo[] = JSON.parse(info);
          
          // 获取作者信息
          const data1 = await Promise.all(data.map(async video => {
            try {
              const userInfo = await fetchOtherUserInfo(video.uploaderID);
              return {
                ...video,
                uploaderInfo: userInfo
              };
            } catch (error) {
              console.log("无法获取UP主信息", error);
              return video; // 保持原样，没有 uploaderInfo
            }
          }));


          // 如果不足指定数量，填充空数据（函数式实现）
          const fillCount = Math.max(0, count - data1.length);
          const filledData = data1.concat(
            Array.from({ length: fillCount }, () => ({
              videoID: 0,
              title: "暂无视频",
              cover: DefaultCover,
              duration: 0,
              description: "",
              uploaderID: 0,
              likes: 0,
              favorites: 0,
              views: 0,
              uploadTime: new Date().toISOString()
            }))
          );

          resolve(filledData);
        } catch (error) {
          reject(error);
        }
      },
      (error: string) => {
        reject(new Error(error));
      }
    );
  });
}