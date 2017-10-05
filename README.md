# ImageLearning
<img align="right" src="https://github.com/readbeard/ImageLearning/blob/master/github_logo.png">
ImageLearning it's and Android Applications developed for educational purposes.
By recognizing subjects of taken pictures, this application can make the user learn 
new terms in the languages that he preferes. Also, the user can listen to words in order
to memorize the pronunciation of it and improve his knowledge not only in writing.
Finally users can directly search on Google the word the they prefer and browse directly
inside the application 
<br></br>
<br></br>

## Features
First of all, when app launched the first time, a nice tutorial presenting the main features in a pretty way is shown. This tutorial can be launched by user clicking on the help button on the bottom left of the picture, in the main view of the application, as shown in the following figure.
ImageLearning allows the user to take a picture, and gives him back some buttons displaying terms related to the subject of the photo. The data is retrieved by two providers (see below) and if a match between their calculations is found, it is shown in a light green button. It also provides a translation of the given terms, to make the user learn how to pronounce the term in different languages. In fact, a user can click on a button and listen to the pronunciation of the word. To change the language of the contents of the screen, a white button on the bottom right  of the picture is shown. From there, a user can select from a list of languages the one that he prefers.
At this state of the art, the application supports 5 languages: English (UK), Italian, Spanish, French and German.
![Image of Screens](https://github.com/readbeard/ImageLearning/blob/master/github_imagelearning.png)
### But that’s not all! 
Together with the calculated words about the picture, ImageLearning provides a graph showing
the confidence value of that particular term. This value is a number in the [0,1] interval, 
that represents the accuracy of the word detection in the image. 
Informally, for example, for a picture in which the "Coliseum" entity is detected, 
confidence represents the confidence that there is a amphitheater in the query image.

## Technologies used
ImageLearning exploits sotware services given by Google and Microsoft.
<img align="right" src="https://github.com/readbeard/ImageLearning/blob/master/github_technologies.png">
In particular, it relies on Services Google Cloud Vision API and Microsoft Computer Vision API.
Among all other possibilities, those two providers give the ability to send an image to their cluster of servers 
that run machine learning based algorithm able to make an estimate about the contents of it, recognizing, with an interval 
of confidence, the subject of the photo (formally, they’re “powerful image capabilities and easy to use APIs” ). 
Those services are named, respectively, Google Cloud Vision and Microsoft Computer Vision.
The calls exposed by both the services are RESTful, and their simplicity and free-to-use first period are the main advantage 
of usage.
<br></br>
<br></br>
## Libraries used
The application exploits a some libraries that allow an easier and nicer representation of the data to be presented. In particular, the dependencies of the app include:


- *PhilJay / MPAndroidChart* ( that can be found at https://github.com/PhilJay/MPAndroidChart). A very easy to use library that cares of taking data and display them in a graph. It supports multiple kind of graphs (in this case, a barChart), providing high flexibility in terms of retrieving data, operate with them and show them in a customizable pretty way.
- *ApmeM / Android-flowlayout* (that can be found at: https://github.com/ApmeM/android-flowlayout ). A library that facilitates displaying views automatically placing them in a way to better exploit the whole screen. Used, in this app, to use all the space available to show buttons containing words.
- *apl-devs/AppIntro* (that can be found at https://github.com/apl-devs/AppIntro ). Another easy to use library that allows android developers to build their own first-launch tutorial and make their app more and more google-styled. In this application, it was used for this purposes.
