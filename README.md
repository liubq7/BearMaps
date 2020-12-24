# Bear Maps  

Bear Maps is a web mapping application for the greater Berkeley area with functions of rastering, auto-complete search, navigation by implementing **Graph**, **Trie**, **K-d Tree** and **A\* algorithm** from scratch in Java.  
**Online Demo**: http://bearmaps-liubq-7.herokuapp.com/  

<img src="demo.gif" alt="demo_gif" width="100%"/>

## Features  
* Map Rastering: Find the grid of images that best matche user's requested area and level of zoom. Then render these images on the browser.  
* Routing & Location Data: Use a real world dataset combined with an industrial strength dataset parser to construct a graph. This graph contains all location data and route information.
* Nearest Location: Get the nearest location of the mouse double click point by k-d tree.  
* Route Search: Find the shortest path between start point and end point using A* algorithm.  
* Autocompletion: Autocomplete the partial query string in search box by trie.  
* Search: The user is able to search for places of interest.  

## Acknowledgments
* Ideas and front-end code from [CS61B](https://sp18.datastructur.es/materials/proj/proj3/proj3)  
* Data from [OpenStreetMap](http://www.openstreetmap.org/)