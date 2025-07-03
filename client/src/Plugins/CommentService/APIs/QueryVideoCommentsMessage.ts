/**
 * QueryVideoCommentsMessage
 * desc: 用于分页获取一个视频下的所有评论。
 * @param videoId: Int (视频的唯一标识符)
 * @param rangeL: Int (评论分页的开始范围)
 * @param rangeR: Int (评论分页的结束范围)
 * @return comments: Comment:1140 (查询到的评论列表，每个评论包含评论内容及相关信息)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryVideoCommentsMessage extends TongWenMessage {
    constructor(
        public  videoId: number,
        public  rangeL: number,
        public  rangeR: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Comment"]
    }
}

