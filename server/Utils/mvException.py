import os
import re
import argparse
from pathlib import Path

def replace_exceptions_in_file(file_path, dry_run=False):
    """替换文件中以Exception结尾的类型为InvalidInputException"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 查找所有以Exception结尾的类型（排除APIException自身）
    pattern = r'\b([A-Za-z][\w\.]*Exception)\b'
    matches = set(re.findall(pattern, content))
    
    # 过滤掉APIException和已知不需要替换的类型
    exclude = {'InvalidInputException', 'Exception', 'Throwable'}
    to_replace = [m for m in matches if m not in exclude and not m.startswith('Common.APIException')]
    
    if not to_replace:
        return 0

    has_import = re.search(r'import\s+Common\.APIException\.InvalidInputException\b', content) is not None
    modified_content = content
    if not has_import:
        # 查找import的位置
        package_match = re.search(r'^import\s+[\w\.]+$', content, re.MULTILINE)
        package_end = package_match.end()
        print(package_end)
        modified_content = (
            content[:package_end] +
            '\nimport Common.APIException.InvalidInputException' +
            content[package_end:]
        )
    replace_count = 0
    
    # 逐个替换匹配项
    for exception_type in to_replace:
        # 创建替换模式，确保不会替换部分匹配的情况
        type_pattern = r'\b' + re.escape(exception_type) + r'\b'
        modified_content, count = re.subn(type_pattern, 'InvalidInputException', modified_content)
        replace_count += count
    
    # 输出替换信息
    print(f"📝 {file_path}:")
    print(f"   Found exceptions: {', '.join(to_replace)}")
    print(f"   Total replacements: {replace_count}")
    
    # 实际修改文件（如果不是干跑模式）
    if not dry_run:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(modified_content)
    
    return replace_count

def find_and_replace_exceptions(root_dir, dry_run=False):
    """在指定目录结构中查找并替换异常类型"""
    total_files = 0
    total_replacements = 0
    
    # 查找所有以Service结尾的顶级目录
    service_dirs = [d for d in os.listdir(root_dir) 
                   if d.endswith('Service') and os.path.isdir(os.path.join(root_dir, d))]
    
    if not service_dirs:
        print(f"⚠️ No *Service directories found in {root_dir}")
        return
    
    print(f"🔍 Found {len(service_dirs)} service directories:")
    for service_dir in service_dirs:
        print(f"  - {service_dir}")
    
    # 在每个Service目录中查找目标子目录
    for service_dir in service_dirs:
        service_path = os.path.join(root_dir, service_dir)
        
        # 目标子目录：Impl 和 Process
        target_subdirs = []
        for subdir in ['Impl', 'Utils']:
            subdir_path = os.path.join(service_path, 'src', 'main', 'scala', subdir)
            if os.path.exists(subdir_path):
                target_subdirs.append(subdir_path)
        
        if not target_subdirs:
            print(f"⚠️ No Impl/Process directories found in {service_dir}")
            continue
        
        # 在每个目标子目录中查找Scala文件
        for subdir_path in target_subdirs:
            print(f"\n🔍 Processing {subdir_path}...")
            for root, _, files in os.walk(subdir_path):
                for file in files:
                    if file.endswith('.scala') and os.path.splitext(file)[0] not in ['Init', 'ProcessUtils', 'Routes', 'Server']:
                        file_path = os.path.join(root, file)
                        total_files += 1
                        replacements = replace_exceptions_in_file(file_path, dry_run)
                        total_replacements += replacements
    
    print("\n" + "="*50)
    print(f"✅ Scan completed!")
    print(f"   Service directories: {len(service_dirs)}")
    print(f"   Scala files scanned: {total_files}")
    print(f"   Total replacements: {total_replacements}")
    
    if dry_run:
        print("\n⚠️ DRY RUN MODE: No files were modified. Use --execute to apply changes")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Replace exception types with APIException in Scala files')
    parser.add_argument('--root', default='.', 
                       help='Root directory to search (default: current directory)')
    parser.add_argument('--execute', action='store_true',
                       help='Actually modify files (without this flag, only show what would be changed)')
    args = parser.parse_args()
    
    print(f"🚀 Starting exception replacement in: {args.root}")
    # file_path = ".\\UserService\\src\\main\\scala\\Impl\\ModifyUserInfoMessagePlanner.scala"
    # replace_exceptions_in_file(file_path, dry_run=not args.execute)
    find_and_replace_exceptions(args.root, dry_run=not args.execute)