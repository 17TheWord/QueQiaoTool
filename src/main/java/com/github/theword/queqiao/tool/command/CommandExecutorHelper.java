package com.github.theword.queqiao.tool.command;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandExecutorHelper {

    private final RootCommand rootCommand;

    public CommandExecutorHelper() {
        this.rootCommand = new RootCommand();
    }

    public RootCommand getRootCommand() {
        return rootCommand;
    }

    /**
     * 执行命令
     *
     * @param sender 命令执行者
     * @param args   命令参数
     */
    public void execute(Object sender, String[] args) {
        if (args.length == 0) {
            rootCommand.execute(sender, new ArrayList<>());
            return;
        }

        SubCommand current = rootCommand;
        int index = 0;

        // 尝试逐层向下查找匹配的子命令
        while (index < args.length) {
            String arg = args[index];
            SubCommand next = null;

            for (SubCommand child : current.getChildren()) {
                if (child.getName().equalsIgnoreCase(arg)) {
                    next = child;
                    break;
                }
            }

            if (next != null) {
                current = next;
                index++;
            } else {
                // 找不到匹配的子命令，停止查找
                break;
            }
        }

        // 将剩余的参数传递给最终找到的命令
        // 例如 /queqiao client list，匹配到 list 命令，剩余参数为空
        // 例如 /queqiao client reconnect all，匹配到 reconnect 命令，剩余参数为 ["all"] (假设 reconnect 没有子命令 all)
        List<String> remainingArgs = new ArrayList<>();
        if (index < args.length) {
            remainingArgs.addAll(Arrays.asList(args).subList(index, args.length));
        }

        current.execute(sender, remainingArgs);
    }

    /**
     * Tab 补全
     *
     * @param sender 命令执行者
     * @param args   命令参数
     * @return 补全列表
     */
    public List<String> tabComplete(Object sender, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        SubCommand current = rootCommand;
        // 遍历到倒数第二个参数，找到“当前正在输入的参数”的父命令
        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            boolean found = false;
            for (SubCommand child : current.getChildren()) {
                if (child.getName().equalsIgnoreCase(arg)) {
                    current = child;
                    found = true;
                    break;
                }
            }
            if (!found) {
                // 路径中断，无法补全
                return Collections.emptyList();
            }
        }

        // 最后一个参数是用户正在输入的内容
        String lastArg = args[args.length - 1].toLowerCase();

        // 返回匹配前缀的子命令名称
        return current.getChildren().stream().map(SubCommand::getName).filter(name -> name.toLowerCase().startsWith(lastArg)).collect(Collectors.toList());
    }
}
