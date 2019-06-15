;; room for improvement on my "build tool" here (but that lickety-split repl is so nice...)
(.mkdir (java.io.File. "./classes/"))
(compile 'noah.serdes)
