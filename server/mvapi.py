import os
import glob
import shutil

# 配置根路径
SERVER_DIR = "."
APIS_PATH = os.path.join("src", "main", "scala", "APIs")


def sync_api_files():
    # 获取所有微服务目录
    services = [
        d for d in os.listdir(SERVER_DIR) if os.path.isdir(os.path.join(SERVER_DIR, d))
    ]

    print(f"发现微服务目录: {', '.join(services)}")

    # for consumer in services:
    #     consumer_dir = os.path.join(SERVER_DIR, consumer)
    #     consumer_apis_dir = os.path.join(consumer_dir, APIS_PATH)

    #     if not os.path.exists(consumer_apis_dir):
    #         print(f"跳过 {consumer} - 没有找到API目录: {consumer_apis_dir}")
    #         continue

    #     print(f"\n处理服务: {consumer}")
    #     print(f"API目录: {consumer_apis_dir}")

    #     # 获取当前服务依赖的其他服务API目录
    #     for provider in os.listdir(consumer_apis_dir):
    #         # 跳过当前服务自己的API目录
    #         if provider == consumer:
    #             continue

    #         provider_api_dir = os.path.join(consumer_apis_dir, provider)
    #         if not os.path.isdir(provider_api_dir):
    #             continue

    #         # 获取提供者服务的源API目录
    #         provider_source_dir = os.path.join(
    #             SERVER_DIR, provider, APIS_PATH, provider
    #         )
    #         if not os.path.exists(provider_source_dir):
    #             print(f"  ! 提供者 {provider} 源目录不存在: {provider_source_dir}")
    #             continue

    #         # 处理所有目标文件
    #         for target_file in glob.glob(
    #             os.path.join(provider_api_dir, "**"), recursive=True
    #         ):
    #             if not os.path.isfile(target_file):
    #                 continue

    #             # 计算对应的源文件路径
    #             rel_path = os.path.relpath(target_file, provider_api_dir)
    #             source_file = os.path.join(provider_source_dir, rel_path)

    #             if not os.path.exists(source_file):
    #                 print(f"    ! 源文件不存在: {source_file}")
    #                 continue

    #             # 打印将要执行的操作
    #             print(f"用 {source_file} 覆盖 {target_file}")

    #             # 实际执行时取消以下注释
    #             os.system(f'git rm -f "{target_file}"')
    #             # 确保目标目录存在
    #             target_dir = os.path.dirname(target_file)
    #             os.makedirs(target_dir, exist_ok=True)
    #             shutil.copy(source_file, target_file)
    #             # os.system(f'git add "{target_file}"')

    for consumer in services:
        consumer_dir = os.path.join(SERVER_DIR, consumer)
        consumer_apis_dir = os.path.join(consumer_dir, APIS_PATH)

        if not os.path.exists(consumer_apis_dir):
            print(f"跳过 {consumer} - 没有找到API目录: {consumer_apis_dir}")
            continue

        print(f"\n处理服务: {consumer}")
        print(f"API目录: {consumer_apis_dir}")

        # 获取当前服务依赖的其他服务API目录
        for provider in os.listdir(consumer_apis_dir):
            # 跳过当前服务自己的API目录
            if provider == consumer:
                continue

            provider_api_dir = os.path.join(consumer_apis_dir, provider)

            if not os.listdir(provider_api_dir):
                print(f"  删除空文件夹: {provider_api_dir}")
                os.rmdir(provider_api_dir)


if __name__ == "__main__":
    print("=" * 80)
    print("API 文件同步脚本 (模拟运行模式)")
    print("=" * 80)
    print("此脚本显示将要执行的操作，但不会实际修改文件系统")
    print("要实际执行，请取消脚本中的注释代码\n")

    sync_api_files()

    print("\n操作预览完成！")
    print("请检查以上输出，确认无误后取消脚本中的注释代码来实际执行")
