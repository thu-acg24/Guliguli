/**
 * FeedbackDetail
 * desc: 用户反馈详情
 * @param feedbackID: Int (反馈的唯一标识符)
 * @param userID: Int (用户的唯一标识符)
 * @param like: Boolean (用户是否喜欢该内容)
 * @param favorite: Boolean (用户是否收藏该内容)
 * @param timestamp: DateTime (反馈产生的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class FeedbackDetail extends Serializable {
    constructor(
        public  feedbackID: number,
        public  userID: number,
        public  like: boolean,
        public  favorite: boolean,
        public  timestamp: number
    ) {
        super()
    }
}


