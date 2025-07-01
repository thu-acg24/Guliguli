/**
 * PublishDanmakuMessage
 * desc: 用于发布弹幕功能点
 * @param token: String (用户凭据，用于验证身份是否合法)
 * @param videoID: Int (目标视频的唯一标识符)
 * @param timeInVideo: Float (弹幕出现的时间点（单位：秒）)
 * @param danmakuContent: String (弹幕的文字内容)
 * @param danmakuColor: String (弹幕的颜色值（例如：#FFFFFF）)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class PublishDanmakuMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  timeInVideo: number,
        public  danmakuContent: string,
        public  danmakuColor: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10014"
    }
}

