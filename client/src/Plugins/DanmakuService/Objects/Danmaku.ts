/**
 * Danmaku
 * desc: 弹幕信息
 * @param danmakuID: Int (弹幕的唯一标识ID)
 * @param content: String (弹幕的具体内容)
 * @param videoID: Int (弹幕所属视频的唯一标识ID)
 * @param authorID: Int (发布弹幕的用户的唯一标识ID)
 * @param danmakuColor: String (弹幕的颜色值)
 * @param timeInVideo: Float (弹幕出现在视频中的时间点（秒）)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class Danmaku extends Serializable {
    constructor(
        public  danmakuID: number,
        public  content: string,
        public  videoID: number,
        public  authorID: number,
        public  danmakuColor: string,
        public  timeInVideo: number
    ) {
        super()
    }
}


