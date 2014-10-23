task :default => :geometryapp

desc "Execute rakefile in GeometryApp folder"
task :geometryapp do
  Dir.chdir 'GeometryApp' do
    fail if !system("rake")
  end
end
