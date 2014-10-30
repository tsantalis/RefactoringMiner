package org.animals;

public class Dog {

	private int age = 0;

	private String ownerName;
	
	private int ownerAge;
	
	public int getAge() {
		return this.age;
	}
	
	public String getOwnerName() {
		return this.ownerName;
	}
	
	public int getOwnerAge() {
		return this.ownerAge;
	}
	
	public void bark() {
		System.out.println("ruff");
		System.out.println("ruff");
		this.takeABreath();
		System.out.println("ruff");
		System.out.println("ruff");
		System.out.println("ruff");
	}

	public void takeABreath() {
		System.out.println("...");
	}

}
