/**
 * VideoInfo
 * desc: 视频的基本信息
 * @param videoID: Int (视频的唯一ID)
 * @param title: String (视频的标题)
 * @param description: String (视频的描述)
 * @param tag: String (视频的标签列表)
 * @param uploaderID: Int (上传者的用户ID)
 * @param views: Int (视频的观看数量)
 * @param likes: Int (视频的点赞数量)
 * @param favorites: Int (视频的收藏数量)
 * @param visible: Boolean (视频是否可见)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class VideoInfo extends Serializable {
    constructor(
        public  videoID: number,
        public  title: string,
        public  description: string,
        public  tag: string[],
        public  uploaderID: number,
        public  views: number,
        public  likes: number,
        public  favorites: number,
        public  visible: boolean
    ) {
        super()
    }
}


