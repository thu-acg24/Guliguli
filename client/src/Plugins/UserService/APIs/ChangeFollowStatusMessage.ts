/**
 * ChangeFollowStatusMessage
 * desc: 创建或删除当前用户和目标用户之间的关注关系, 用于用户关注或取消关注功能点
 * @param token: String (用户登录的Token，用于校验身份)
 * @param followeeID: Int (被关注的目标用户ID)
 * @param isFollow: Boolean (操作类型，true表示关注，false表示取消关注)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class ChangeFollowStatusMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  followeeID: number,
        public  isFollow: boolean
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}

