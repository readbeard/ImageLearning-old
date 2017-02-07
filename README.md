# ImageLearning
ImageLearning it's and Android Applications developed for educational purposes.
By recognizing subjects of taken pictures, this application can make the user learn 
new terms in the languages that he preferes. 
##Features
In particular, the application 
allows the user to take a picture and, by recognizing the subject of it, shows a list
of estimated results. Those results are pretty close to the real object the user 
of terms related to the subject of the photo. It also provides a translation of the given terms, to make the user learn how to pronounce the term in different languages. 
At this state of the art, the application supports 5 languages: English (UK), Italian, Spanish, French and German.

![Image of Screens](https://github.com/readbeard/ImageLearning/blob/master/github_imagelearning.png)
###But that’s not all! 
Together with the calculated words about the picture, ImageLearning provides a graph showing
the confidence value of that particular term. This value is a number in the [0,1] interval, 
that represents the accuracy of the word detection in the image. 
Informally, for example, for a picture in which the "Coliseum" entity is detected, 
confidence represents the confidence that there is a tower in the query image.

##Technologies used
ImageLearning exploits distributed services given by Google and Microsoft. 
In particular, it relies on Platform as a Service (PaaS) Google App Engine and Microsoft Cognitive Services. 
Among all other possibilities, those two providers give the ability to send an image to their cluster of servers 
that run machine learning based algorithm able to make an estimate about the contents of it, recognizing, with an interval 
of confidence, the subject of the photo (formally, they’re “powerful image capabilities and easy to use APIs” ). 
Those services are named, respectively, Google Cloud Vision and Microsoft Computer Vision.
The calls exposed by both the services are RESTful, and their simplicity and free-to-use first period are the main advantage 
of usage.
