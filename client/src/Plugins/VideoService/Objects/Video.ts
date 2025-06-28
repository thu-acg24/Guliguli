/**
 * Video
 * desc: 视频信息的数据结构
 * @param videoID: Int (视频的唯一标识)
 * @param title: String (视频的标题信息)
 * @param description: String (视频的描述信息)
 * @param duration: Int (视频的时长，单位为秒)
 * @param tag: String (视频的标签列表)
 * @param serverPath: String (视频存储在服务器中的路径)
 * @param coverPath: String (视频封面图片的路径)
 * @param uploaderID: Int (上传视频的用户ID)
 * @param views: Int (视频的播放量)
 * @param likes: Int (视频的点赞数)
 * @param favorites: Int (视频的收藏数)
 * @param status: VideoStatus:1022 (视频的审核状态)
 * @param uploadTime: DateTime (视频的上传时间)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'

import { VideoStatus } from 'Plugins/VideoService/Objects/VideoStatus';


export class Video extends Serializable {
    constructor(
        public  videoID: number,
        public  title: string,
        public  description: string,
        public  duration: number,
        public  tag: string[] | null,
        public  serverPath: string,
        public  coverPath: string,
        public  uploaderID: number,
        public  views: number,
        public  likes: number,
        public  favorites: number,
        public  status: VideoStatus,
        public  uploadTime: number
    ) {
        super()
    }
}


