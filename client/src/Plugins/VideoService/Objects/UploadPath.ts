/**
 * UploadPath
 * desc: 视频收藏记录，记录用户收藏视频的相关信息
 * @param coverPath: String (封面上传链接)
 * @param coverToken: String (封面上传会话Token)
 * @param videoPath: String (视频上传链接)
 * @param videoToken: String (视频上传会话Token)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class UploadPath extends Serializable {
    constructor(
        public  coverPath: string,
        public  coverToken: string,
        public  videoPath: string,
        public  videoToken: string,
    ) {
        super()
    }
}


