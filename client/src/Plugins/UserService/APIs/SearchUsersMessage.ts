/**
 * SearchUsersMessage
 * desc: 根据用户昵称搜索用户，返回所有昵称包含输入字符串的用户列表。
 * @param searchString: String (搜索字符串)
 * @return users: List[UserInfo] (搜索返回的用户列表，按昵称字典序排序)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class SearchUsersMessage extends TongWenMessage {
    constructor(
        public  searchString: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

