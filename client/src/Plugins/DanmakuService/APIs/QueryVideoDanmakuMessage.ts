/**
 * QueryVideoDanmakuMessage
 * desc: 用于查询视频弹幕功能点
 * @param videoID: Int (视频的唯一标识符。)
 * @return danmakus: Danmaku[] (与指定视频相关的弹幕记录列表。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryVideoDanmakuMessage extends TongWenMessage {
    constructor(
        public  videoID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Danmaku"]
    }
}

