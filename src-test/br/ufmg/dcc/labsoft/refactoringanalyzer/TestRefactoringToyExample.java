package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestRefactoringToyExample {

	@Test
	public void test() throws Exception {
		TestBuilder test = new TestBuilder();
		test
			.project("https://github.com/danilofes/refactoring-toy-example.git", "master")
			.atNonSpecifiedCommitsContainsNothing()
			// Danilo's refactorings
			.atCommit("36287f7c3b09eff78395267a3ac0d7da067863fd").containsOnly(
				"Pull Up Attribute	private age : int from class org.animals.Labrador to class org.animals.Dog",
				"Pull Up Attribute	private age : int from class org.animals.Poodle to class org.animals.Dog",
				"Pull Up Method	public getAge() : int from class org.animals.Labrador to public getAge() : int from class org.animals.Dog",
				"Pull Up Method	public getAge() : int from class org.animals.Poodle to public getAge() : int from class org.animals.Dog")
			.atCommit("40950c317bd52ea5ce4cf0d19707fe426b66649c").containsOnly(
				"Extract Method	public takeABreath() : void extracted from public bark() : void in class org.animals.Dog")
			.atCommit("63cbed99a601e79c6a0ae389b2a57acdbd3e1b44").containsOnly(
				"Rename Class	org.animals.Cow renamed to org.animals.CowRenamed")
			.atCommit("58495630295833c9d73559bd958c2f95339f9c62").containsOnly(
				"Extract Superclass	org.animals.Bird from classes [org.animals.Chicken, org.animals.Duck]")
			.atCommit("70b71b7fd3c5973511904c468e464d4910597928").containsOnly(
				"Move Class	org.animals.Cat moved to org.felines.Cat")
			.atCommit("05c1e773878bbacae64112f70964f4f2f7944398").containsOnly(
				"Extract Superclass	org.felines.Feline from classes [org.felines.Cat]")
			
			// Thiago's refactorings
			.atCommit("1328d7873efe6caaffaf635424e19a4bb5e786a8").containsOnly(
			    "Extract Interface	org.felines.AnimalSuper from classes [org.felines.Animal]")
			.atCommit("0a46ed5c56c8b1576dfc92f3ec5bc2f0ea68aafe").containsOnly(
			    "Push Down Attribute	protected age : int from class org.reptile.AnimalMarilho to class org.reptile.Reptile",
			    "Push Down Attribute	protected name : int from class org.reptile.AnimalMarilho to class org.reptile.Reptile",
			    "Push Down Method	public getName() : int from class org.reptile.AnimalMarilho to public getName() : int from class org.reptile.Reptile",
			    "Push Down Method	public setName(name int) : void from class org.reptile.AnimalMarilho to public setName(name int) : void from class org.reptile.Reptile")
			.atCommit("638f37ca6b4dcdbb6a4735f93e37445aeef79749").containsOnly(
			    "Push Down Method	public equals(obj Object) : boolean from class org.reptile.Reptile to public equals(obj Object) : boolean from class org.reptile.TurtleMarinha")
			.atCommit("0e193b7d02902c6f2abf7c88eebe937d1ac5fc51").containsOnly(
			    "Push Down Method	public hashCode() : int from class org.reptile.AnimalMarilho to public hashCode() : int from class org.reptile.Reptile")
			.atCommit("b61e75b773f48e680f5bb7362445ba0642c2ee91").containsOnly(
			    "Push Down Method	public equals(obj Object) : boolean from class org.reptile.AnimalMarilho to public equals(obj Object) : boolean from class org.reptile.Reptile")
			.atCommit("6bbfab9e7051362aad9d993f5f6a013b73e75117").containsOnly(
			    "Pull Up Attribute	protected age : int from class org.reptile.Reptile to class org.reptile.AnimalMarilho",
			    "Pull Up Attribute	protected name : int from class org.reptile.Reptile to class org.reptile.AnimalMarilho",
			    "Pull Up Attribute	protected spead : int from class org.reptile.Reptile to class org.reptile.AnimalMarilho",
			    "Pull Up Attribute	protected action : String from class org.reptile.Reptile to class org.reptile.AnimalMarilho",
			    "Pull Up Method	public getName() : int from class org.reptile.Reptile to public getName() : int from class org.reptile.AnimalMarilho",
			    "Pull Up Method	public setName(name int) : void from class org.reptile.Reptile to public setName(name int) : void from class org.reptile.AnimalMarilho",
			    "Pull Up Method	public getSpead() : int from class org.reptile.Reptile to public getSpead() : int from class org.reptile.AnimalMarilho",
			    "Pull Up Method	public setSpead(spead int) : void from class org.reptile.Reptile to public setSpead(spead int) : void from class org.reptile.AnimalMarilho",
			    "Pull Up Method	public getAction() : String from class org.reptile.Reptile to public getAction() : String from class org.reptile.AnimalMarilho",
			    "Pull Up Method	public setAction(action String) : void from class org.reptile.Reptile to public setAction(action String) : void from class org.reptile.AnimalMarilho",
			    "Pull Up Method	public hashCode() : int from class org.reptile.Reptile to public hashCode() : int from class org.reptile.AnimalMarilho",
			    "Pull Up Method	public equals(obj Object) : boolean from class org.reptile.Reptile to public equals(obj Object) : boolean from class org.reptile.AnimalMarilho")
			.atCommit("bbd8dc082406a950adf73b7211c887bfab6480f1").containsOnly(
			    "Extract Superclass	org.reptile.AnimalMarilho from classes [org.reptile.Reptile]")
			.atCommit("9803046111744317efaa65a83e65ce8ceb0c15c2").containsOnly(
			    "Push Down Method	public getAge() : int from class org.reptile.Reptile to public getAge() : int from class org.reptile.TurtleMarinha",
			    "Push Down Method	public setAge(age int) : void from class org.reptile.Reptile to public setAge(age int) : void from class org.reptile.TurtleMarinha")
			.atCommit("a1b3a91d1a423f2b7360e009e47f30aedb663b6f").containsOnly(
			    "Rename Class	org.reptile.Turtle renamed to org.reptile.TurtleMarinha")
			.atCommit("802e21bffe95f0740f44d1a45e3c22adae0ba48c").containsOnly(
			    "Extract Superclass	org.reptile.Reptile from classes [org.reptile.Turtle]")
			.atCommit("3f3552830d3e464f96f99bd55641a7c7b16bdd11").containsOnly(
			    "Push Down Attribute	private speed : int from class org.felines.Feline to class org.felines.Tiger",
			    "Push Down Method	public getSpeed() : int from class org.felines.Feline to public getSpeed() : int from class org.felines.Tiger",
			    "Push Down Method	public setSpeed(speed int) : void from class org.felines.Feline to public setSpeed(speed int) : void from class org.felines.Tiger")
			.atCommit("f35b2c8eb8c320f173237e44d04eefb4634649a2").containsOnly(
			    "Extract Method	private sleepNight() : void extracted from public sleep() : void in class org.felines.Cat")
			.atCommit("c0a051fdeb02fd4374ebe625d6af9e3125a2b9af").containsOnly(
			    "Pull Up Attribute	private speed : int from class org.felines.Tiger to class org.felines.Feline")
//			.atCommit("7ebd3deba1ae42ff1e9c8585fc304839c5288863").containsOnly(
//			    "Pull Up Method	public action() : void from class org.felines.Cat to public action() : void from class org.felines.Feline")
			.atCommit("92b201345f730110445d83f4fefe8ae88bc4872b").containsOnly(
			    "Pull Up Attribute	private age : int from class org.felines.Tiger to class org.felines.Feline",
			    "Pull Up Attribute	private name : int from class org.felines.Tiger to class org.felines.Feline",
			    "Pull Up Method	public getAge() : int from class org.felines.Tiger to public getAge() : int from class org.felines.Feline",
			    "Pull Up Method	public setAge(age int) : void from class org.felines.Tiger to public setAge(age int) : void from class org.felines.Feline",
			    "Pull Up Method	public getName() : int from class org.felines.Tiger to public getName() : int from class org.felines.Feline",
			    "Pull Up Method	public setName(name int) : void from class org.felines.Tiger to public setName(name int) : void from class org.felines.Feline")
			.atCommit("9a9878aeb62a6bb6ff2bed6c03dd1dd7ed1f202b").containsOnly(
			    "Pull Up Method	public meow() : void from class org.felines.Cat to public meow() : void from class org.felines.Feline")
			.atCommit("0c5c24356f3179ee320c3318f91278520caafb3a").containsOnly(
			    "Move Class	org.felines.Feline moved to org.birds.Feline")
//			.atCommit("12b11bf39cb4800e3fa57fb1112c5fbda26de3df").containsOnly(
//			    "Move Class	org.animals.Tiger moved to org.felines.Tiger")
			.atCommit("60226924fead7d0c4646df4f4fd65667e83da6dc").containsOnly(
			    "Move Class	org.animals.Bird moved to org.birds.Bird",
			    "Move Class	org.animals.Chicken moved to org.birds.Chicken",
			    "Move Class	org.animals.Duck moved to org.birds.Duck")
			.atCommit("0bb0526b70870d57cbac9fcc8c4a7346a4ce5879").containsOnly(
				"Rename Method public bark() : void renamed to public barkBark() : void in class org.animals.Dog")
			
			// More refactorings
			.atCommit("9a5c33b16d07d62651ea80552e8782974c96bb8a").containsOnly(
				"Move Attribute public magicNumber : int from class org.DogManager to class org.animals.Dog")
			.atCommit("d4bce13a443cf12da40a77c16c1e591f4f985b47").containsOnly(
				"Move Method public barkBark(manager DogManager) : void from class org.animals.Dog to public barkBark(dog Dog) : void from class org.DogManager")
		;
		test.assertExpectations();
	}

}
