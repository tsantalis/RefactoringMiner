package org.animals;

public class Dog {

	private int age = 0;

	private Owner owner;
	
	public int getAge() {
		return this.age;
	}
	
	public Owner getOwner() {
		return this.owner;
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
