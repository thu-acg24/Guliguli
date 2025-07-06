/**
 * QueryVideoCommentsMessage
 * desc: 用于分页获取一个视频下的所有评论。
 * @param videoID: Int (视频的唯一标识符)
 * @param lastTime: string (已获取的最新一条评论的时间戳，用于分页查询)
 * @param lastID: Int (已获取的最新一条评论的ID，用于分页查询)
 * @param rootID: Int (空则是一级评论，否则获取该一级评论下的二级评论)
 * @return comments: Comment:1140 (查询到的评论列表，每个评论包含评论内容及相关信息)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryVideoCommentsMessage extends TongWenMessage {
    constructor(
        public  videoID: number,
        public  lastTime: number,
        public  lastID: number,
        public  rootID: number | null
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Comment"]
    }
}

