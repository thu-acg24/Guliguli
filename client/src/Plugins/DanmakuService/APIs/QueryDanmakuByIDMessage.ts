/**
 * QueryDanmakuByIDMessage
 * desc: 用于查询弹幕信息功能点
 * @param danmakuID: Int (弹幕的唯一标识符，用于获取具体的弹幕信息)
 * @return danmaku: Danmaku (查询返回的弹幕信息，包括弹幕内容、所属视频、时间点、颜色及作者等)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryDanmakuByIDMessage extends TongWenMessage {
    constructor(
        public  danmakuID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Danmaku"]
    }
}

