package com.github.theword.queqiao.tool.command;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.CommandConstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 子命令抽象类
 *
 * <p>采用树形结构管理命令层级关系
 * <p>所有命令均需继承此类
 *
 * @since 0.5.0
 */
public abstract class SubCommand {

    /**
     * 父命令节点
     */
    protected SubCommand parent;

    /**
     * 子命令列表
     */
    protected final List<SubCommand> children = new ArrayList<>();

    /**
     * 添加子命令
     *
     * @param child 子命令
     */
    public void addChild(SubCommand child) {
        child.parent = this;
        this.children.add(child);
    }

    /**
     * 获取所有子命令
     *
     * @return 子命令列表
     */
    public List<SubCommand> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * 获取父命令
     *
     * @return 父命令节点
     */
    public SubCommand getParent() {
        return parent;
    }

    /**
     * 判断是否为根命令
     *
     * @return 是否为根命令
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * 获取完整命令路径
     *
     * <p>示例：/queqiao client list
     *
     * @return 完整命令路径
     */
    public String getFullPath() {
        if (parent == null) {
            return "/" + getName();
        }

        List<String> path = new ArrayList<>();
        SubCommand current = this;
        while (current != null) {
            path.add(0, current.getName());
            current = current.parent;
        }

        return "/" + String.join(" ", path);
    }

    /**
     * 获取完整权限节点
     *
     * <p>从根节点开始拼接，示例：queqiao.client.list
     *
     * @return 完整权限节点
     */
    public String getFullPermissionNode() {
        List<String> path = new ArrayList<>();
        SubCommand current = this;
        while (current != null) {
            path.add(0, current.getName());
            current = current.parent;
        }
        return String.join(".", path);
    }

    /**
     * 获取命令名称
     *
     * @return 命令名称
     */
    public abstract String getName();

    /**
     * 获取命令描述
     *
     * @return 命令描述
     */
    public abstract String getDescription();

    /**
     * 获取命令用法
     *
     * <p>默认返回完整路径，子类可以覆盖以添加参数说明
     *
     * @return 命令用法
     */
    public String getUsage() {
        return getFullPath();
    }

    /**
     * 获取命令权限节点
     *
     * <p>默认返回完整权限节点，子类可以覆盖以自定义权限
     *
     * @return 权限节点
     */
    public String getPermissionNode() {
        return getFullPermissionNode();
    }

    /**
     * 执行命令
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     * @since 0.5.0
     */
    public int execute(Object commandReturner, List<String> args) {
        try {
            if (!GlobalContext.getHandleCommandReturnMessageService().hasPermission(commandReturner, getPermissionNode())) {
                GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(commandReturner, "您没有权限执行此命令。");
                return CommandConstant.FAIL_SIGNAL;
            }
            GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(commandReturner, "============ 鹊桥 ===========");
            onExecute(commandReturner, args);
            GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(commandReturner, "============================");
            return CommandConstant.SUCCESS_SIGNAL;
        } catch (Exception e) {
            GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(commandReturner, "命令执行出错: " + e.getMessage());
            GlobalContext.getLogger().error("命令执行出错", e);
            return CommandConstant.FAIL_SIGNAL;
        }
    }

    /**
     * 执行命令逻辑
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     */
    protected abstract void onExecute(Object commandReturner, List<String> args);


    /**
     * 递归发送指定命令以及所有子命令的树形结构
     *
     * @param commandReturner 命令执行者
     * @param command         当前命令节点
     */
    public void sendCommandTree(Object commandReturner, SubCommand command) {
        String msg = String.format("%s - %s", command.getUsage(), command.getDescription());
        GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(commandReturner, msg);

        for (SubCommand child : command.getChildren()) {
            sendCommandTree(commandReturner, child);
        }
    }

}
