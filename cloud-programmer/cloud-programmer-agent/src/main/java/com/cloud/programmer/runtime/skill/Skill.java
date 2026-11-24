package com.cloud.programmer.runtime.skill;


public class Skill {

    /**
     * skill名称
     */
    private String name;


    /**
     * 描述
     */
    private String description;


    /**
     * 给LLM的指令
     */
    private String instruction;


    public Skill() {
    }


    public Skill(String name, String description, String instruction) {
        this.name = name;
        this.description = description;
        this.instruction = instruction;
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public String getInstruction() {
        return instruction;
    }


    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }
}