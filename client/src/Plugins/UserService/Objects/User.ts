/**
 * User
 * desc: 用户数据结构，用于存储用户的基本信息和状态
 * @param userID: Int (用户的唯一ID)
 * @param username: String (用户名)
 * @param email: String (用户的邮箱地址)
 * @param passwordHash: String (密码的哈希值)
 * @param avatarPath: String (用户头像的存储路径)
 * @param userRole: UserRole:1084 (用户的角色)
 * @param videoCount: Int (用户上传的视频数量)
 * @param isBanned: Boolean (用户是否被禁用)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'

import { UserRole } from 'Plugins/UserService/Objects/UserRole';


export class User extends Serializable {
    constructor(
        public  userID: number,
        public  username: string,
        public  email: string,
        public  passwordHash: string,
        public  avatarPath: string,
        public  userRole: UserRole,
        public  videoCount: number,
        public  isBanned: boolean
    ) {
        super()
    }
}


