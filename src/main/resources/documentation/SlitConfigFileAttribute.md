**SplitConfigFileAttribute**

> The SplitConfigFileAttribute is an easy way to have your Attributes be
> saved in config files which allows for easy usage of end users and
> looks clean in your code.
>
> The SplitConfigFileAttribute saves each attribute in its own file.
>

> Now here are the basics with use cases
>
>
>The following code Example shows an example of how you can declare a new Instance
>```
>SplitConfigFileAttribute<String> stringAttribute = new SplitConfigFileAttribute<>("Hello World", "HelloWorld", String.class);
>```
>The first parameter is the default value that this attribute initially will have.
>
>The second one declares the path of the file in which this attribute is to be save (all paths are starting at /config)
>
> The third parameter is the Class of the Value since it is required to deserialize the value
>
> Notice every Path should only be used once and the value has to be serializable by Gson
>
> ------
>  To retrieve the value we call:
>
> ````
> stringAttribute.getValue();
> ````
> This method get the current value of this attribute
>
> ----------------------------------------
> To load the value from the file we call:
> ````
> stringAttribute.reload();
> ````
>
> -----
> To set a value we can call:
> ````
> stringAttribute.set("Bye World");
> ````
>
> This will set the value to the new Value
>
>