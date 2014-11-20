package br.ufmg.dcc.labsoft.refactoringanalyzer;

import org.junit.Test;

public class TestRevisionsDiff {

	@Test
	public void testRevisionsDiff() throws Exception {
		GitService gitService = new GitServiceImpl();
		gitService.cloneIfNotExists("tmp/refactoring-toy-example", "https://github.com/danilofes/refactoring-toy-example.git");
		
		RefactoringMatcher matcher = new RefactoringMatcher();
		
		// Refactorings iniciais
		matcher.expectAtCommit("36287f7c3b09eff78395267a3ac0d7da067863fd",
			"Pull Up Attribute	private age : int from class org.animals.Labrador to class org.animals.Dog",
			"Pull Up Attribute	private age : int from class org.animals.Poodle to class org.animals.Dog",
			"Pull Up Operation	public getAge() : int from class org.animals.Labrador to public getAge() : int from class org.animals.Dog",
			"Pull Up Operation	public getAge() : int from class org.animals.Poodle to public getAge() : int from class org.animals.Dog");
		matcher.expectAtCommit("40950c317bd52ea5ce4cf0d19707fe426b66649c",
			"Extract Operation	public takeABreath() : void extracted from public bark() : void in class org.animals.Dog");
		matcher.expectAtCommit("63cbed99a601e79c6a0ae389b2a57acdbd3e1b44",
			"Rename Class	org.animals.Cow renamed to org.animals.CowRenamed");
		matcher.expectAtCommit("58495630295833c9d73559bd958c2f95339f9c62",
			"Extract Superclass	org.animals.Bird from classes [org.animals.Chicken, org.animals.Duck]");
		matcher.expectAtCommit("70b71b7fd3c5973511904c468e464d4910597928",
			"Move Class	org.animals.Cat moved to org.felines.Cat");
		matcher.expectAtCommit("05c1e773878bbacae64112f70964f4f2f7944398",
			"Extract Superclass	org.felines.Feline from classes [org.felines.Cat]");
		
		// Refactorings do Thiago
		matcher.expectAtCommit("1328d7873efe6caaffaf635424e19a4bb5e786a8",
		    "Extract Interface	org.felines.AnimalSuper from classes [org.felines.Animal]");
		matcher.expectAtCommit("0a46ed5c56c8b1576dfc92f3ec5bc2f0ea68aafe",
		    "Push Down Attribute	protected age : int from class org.reptile.AnimalMarilho to class org.reptile.Reptile",
		    "Push Down Attribute	protected name : int from class org.reptile.AnimalMarilho to class org.reptile.Reptile",
		    "Push Down Operation	public getName() : int from class org.reptile.AnimalMarilho to public getName() : int from class org.reptile.Reptile",
		    "Push Down Operation	public setName(name int) : void from class org.reptile.AnimalMarilho to public setName(name int) : void from class org.reptile.Reptile");
		matcher.expectAtCommit("638f37ca6b4dcdbb6a4735f93e37445aeef79749",
		    "Push Down Operation	public equals(obj Object) : boolean from class org.reptile.Reptile to public equals(obj Object) : boolean from class org.reptile.TurtleMarinha");
		matcher.expectAtCommit("0e193b7d02902c6f2abf7c88eebe937d1ac5fc51",
		    "Push Down Operation	public hashCode() : int from class org.reptile.AnimalMarilho to public hashCode() : int from class org.reptile.Reptile");
		matcher.expectAtCommit("b61e75b773f48e680f5bb7362445ba0642c2ee91",
		    "Push Down Operation	public equals(obj Object) : boolean from class org.reptile.AnimalMarilho to public equals(obj Object) : boolean from class org.reptile.Reptile");
		matcher.expectAtCommit("6bbfab9e7051362aad9d993f5f6a013b73e75117",
		    "Pull Up Attribute	protected age : int from class org.reptile.Reptile to class org.reptile.AnimalMarilho",
		    "Pull Up Attribute	protected name : int from class org.reptile.Reptile to class org.reptile.AnimalMarilho",
		    "Pull Up Attribute	protected spead : int from class org.reptile.Reptile to class org.reptile.AnimalMarilho",
		    "Pull Up Attribute	protected action : String from class org.reptile.Reptile to class org.reptile.AnimalMarilho",
		    "Pull Up Operation	public getName() : int from class org.reptile.Reptile to public getName() : int from class org.reptile.AnimalMarilho",
		    "Pull Up Operation	public setName(name int) : void from class org.reptile.Reptile to public setName(name int) : void from class org.reptile.AnimalMarilho",
		    "Pull Up Operation	public getSpead() : int from class org.reptile.Reptile to public getSpead() : int from class org.reptile.AnimalMarilho",
		    "Pull Up Operation	public setSpead(spead int) : void from class org.reptile.Reptile to public setSpead(spead int) : void from class org.reptile.AnimalMarilho",
		    "Pull Up Operation	public getAction() : String from class org.reptile.Reptile to public getAction() : String from class org.reptile.AnimalMarilho",
		    "Pull Up Operation	public setAction(action String) : void from class org.reptile.Reptile to public setAction(action String) : void from class org.reptile.AnimalMarilho",
		    "Pull Up Operation	public hashCode() : int from class org.reptile.Reptile to public hashCode() : int from class org.reptile.AnimalMarilho",
		    "Pull Up Operation	public equals(obj Object) : boolean from class org.reptile.Reptile to public equals(obj Object) : boolean from class org.reptile.AnimalMarilho");
		matcher.expectAtCommit("bbd8dc082406a950adf73b7211c887bfab6480f1",
		    "Extract Superclass	org.reptile.AnimalMarilho from classes [org.reptile.Reptile]");
		matcher.expectAtCommit("9803046111744317efaa65a83e65ce8ceb0c15c2",
		    "Push Down Operation	public getAge() : int from class org.reptile.Reptile to public getAge() : int from class org.reptile.TurtleMarinha",
		    "Push Down Operation	public setAge(age int) : void from class org.reptile.Reptile to public setAge(age int) : void from class org.reptile.TurtleMarinha");
		matcher.expectAtCommit("a1b3a91d1a423f2b7360e009e47f30aedb663b6f",
		    "Rename Class	org.reptile.Turtle renamed to org.reptile.TurtleMarinha");
		matcher.expectAtCommit("802e21bffe95f0740f44d1a45e3c22adae0ba48c",
		    "Extract Superclass	org.reptile.Reptile from classes [org.reptile.Turtle]");
		matcher.expectAtCommit("3f3552830d3e464f96f99bd55641a7c7b16bdd11",
		    "Push Down Attribute	private speed : int from class org.felines.Feline to class org.felines.Tiger",
		    "Push Down Operation	public getSpeed() : int from class org.felines.Feline to public getSpeed() : int from class org.felines.Tiger",
		    "Push Down Operation	public setSpeed(speed int) : void from class org.felines.Feline to public setSpeed(speed int) : void from class org.felines.Tiger");
		matcher.expectAtCommit("f35b2c8eb8c320f173237e44d04eefb4634649a2",
		    "Extract Operation	private sleepNight() : void extracted from public sleep() : void in class org.felines.Cat");
		matcher.expectAtCommit("c0a051fdeb02fd4374ebe625d6af9e3125a2b9af",
		    "Pull Up Attribute	private speed : int from class org.felines.Tiger to class org.felines.Feline");
		matcher.expectAtCommit("7ebd3deba1ae42ff1e9c8585fc304839c5288863",
		    "Pull Up Operation	public action() : void from class org.felines.Cat to public action() : void from class org.felines.Feline");
		matcher.expectAtCommit("92b201345f730110445d83f4fefe8ae88bc4872b",
		    "Pull Up Attribute	private age : int from class org.felines.Tiger to class org.felines.Feline",
		    "Pull Up Attribute	private name : int from class org.felines.Tiger to class org.felines.Feline",
		    "Pull Up Operation	public getAge() : int from class org.felines.Tiger to public getAge() : int from class org.felines.Feline",
		    "Pull Up Operation	public setAge(age int) : void from class org.felines.Tiger to public setAge(age int) : void from class org.felines.Feline",
		    "Pull Up Operation	public getName() : int from class org.felines.Tiger to public getName() : int from class org.felines.Feline",
		    "Pull Up Operation	public setName(name int) : void from class org.felines.Tiger to public setName(name int) : void from class org.felines.Feline");
		matcher.expectAtCommit("9a9878aeb62a6bb6ff2bed6c03dd1dd7ed1f202b",
		    "Pull Up Operation	public meow() : void from class org.felines.Cat to public meow() : void from class org.felines.Feline");
		matcher.expectAtCommit("0c5c24356f3179ee320c3318f91278520caafb3a",
		    "Move Class	org.felines.Feline moved to org.birds.Feline");
		matcher.expectAtCommit("12b11bf39cb4800e3fa57fb1112c5fbda26de3df",
		    "Move Class	org.animals.Tiger moved to org.felines.Tiger");
		matcher.expectAtCommit("60226924fead7d0c4646df4f4fd65667e83da6dc",
		    "Move Class	org.animals.Bird moved to org.birds.Bird",
		    "Move Class	org.animals.Chicken moved to org.birds.Chicken",
		    "Move Class	org.animals.Duck moved to org.birds.Duck");

		new RefactoringDetectorImpl().detectAll("tmp/refactoring-toy-example", matcher);

		matcher.checkFalseNegatives();
	}

}
