/**
 * UserInfo
 * desc: 用户信息，包含基本的用户名、头像和封禁状态
 * @param userID: Int (用户的唯一ID)
 * @param username: String (用户名)
 * @param avatarPath: String (用户头像的存储路径)
 * @param isBanned: Boolean (用户是否被封禁)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class UserInfo extends Serializable {
    constructor(
        public  userID: number,
        public  username: string,
        public  avatarPath: string,
        public  isBanned: boolean
    ) {
        super()
    }
}


